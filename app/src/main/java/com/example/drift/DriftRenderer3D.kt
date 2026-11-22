package com.example.drift

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Native OpenGL ES 2.0 renderer powering both the Garage 3D Turntable and Drift Masters 3D Race Canvas.
 */
class DriftRenderer3D(
    private val mode: Mode,
    private val input: DriftControlInput = DriftControlInput(),
    private val audioEngine: DriftAudioEngine? = null,
    private val onHudUpdate: ((DriftHudSnapshot) -> Unit)? = null
) : GLSurfaceView.Renderer {

    enum class Mode {
        TURNTABLE,
        RACE
    }

    // Dynamic configuration synchronized from UI thread
    @Volatile var trackId: TrackId = TrackId.JAPAN
    @Volatile var carColorInt: Int = 0xFFE62424.toInt()
    @Volatile var carStyle: CarStyle = CarStyle.COUPE
    @Volatile var botCount: Int = 1
    @Volatile var canDrive: Boolean = false
    @Volatile var isGhostActive: Boolean = false
    @Volatile var ghostReplay: List<GhostFrame> = emptyList()

    private var builtTrackId: TrackId? = null
    private var builtBotCount: Int = -1
    private var builtCarColor: Int = 0
    private var builtCarStyle: CarStyle? = null

    // GL Program & Uniform/Attribute handles
    private var program = 0
    private var aPos = 0
    private var aNormal = 0
    private var aColor = 0
    private var uMvp = 0
    private var uModel = 0
    private var uColorTint = 0
    private var uFogColor = 0
    private var uFogDensity = 0
    private var uEmissive = 0

    // Matrices (zero allocation in render loop)
    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Reusable meshes
    private class MeshBuffer(
        val buffer: FloatBuffer,
        val vertexCount: Int,
        val drawMode: Int = GLES20.GL_TRIANGLES
    ) {
        fun draw(aPos: Int, aNormal: Int, aColor: Int) {
            val stride = 9 * 4 // 3 pos + 3 normal + 3 color floats
            buffer.position(0)
            GLES20.glEnableVertexAttribArray(aPos)
            GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, stride, buffer)

            buffer.position(3)
            GLES20.glEnableVertexAttribArray(aNormal)
            GLES20.glVertexAttribPointer(aNormal, 3, GLES20.GL_FLOAT, false, stride, buffer)

            buffer.position(6)
            GLES20.glEnableVertexAttribArray(aColor)
            GLES20.glVertexAttribPointer(aColor, 3, GLES20.GL_FLOAT, false, stride, buffer)

            GLES20.glDrawArrays(drawMode, 0, vertexCount)
        }
    }

    private data class CarRenderRig(
        val chassisMesh: MeshBuffer,
        val wheelMesh: MeshBuffer,
        val frontWheelZ: Float,
        val rearWheelZ: Float,
        val wheelHalfTrack: Float,
        val wheelY: Float
    )

    // Scene Meshes
    private var turntablePlatformMesh: MeshBuffer? = null
    private var turntableRingMesh: MeshBuffer? = null
    private var turntableGridMesh: MeshBuffer? = null
    private var particleCubeMesh: MeshBuffer? = null

    private var groundMesh: MeshBuffer? = null
    private var roadMesh: MeshBuffer? = null
    private var curbMesh: MeshBuffer? = null
    private var archMesh: MeshBuffer? = null
    private var polesMesh: MeshBuffer? = null
    private var bulbsMesh: MeshBuffer? = null
    private var playerRig: CarRenderRig? = null
    private var botRigs: List<CarRenderRig> = emptyList()
    private var ghostRig: CarRenderRig? = null

    // Floating particles in turntable
    private val turntableParticles = Array(36) {
        floatArrayOf(
            (Random.nextFloat() - 0.5f) * 4.4f,
            Random.nextFloat() * 2.8f,
            (Random.nextFloat() - 0.5f) * 4.4f
        )
    }

    // Tire smoke pool in race mode
    private val smokePoolSize = 48
    private val smokePositions = Array(smokePoolSize) { floatArrayOf(0f, -999f, 0f, 0f) } // x,y,z,life
    private var smokeCursor = 0

    // Physics & Race State
    private var curve: ClosedCatmullRomCurve3D = ClosedCatmullRomCurve3D(TrackCatalog.get(TrackId.JAPAN).controlPoints)
    private var carX = 0f
    private var carZ = 0f
    private var heading = 0f
    private var velX = 0f
    private var velZ = 0f
    private var steerVisual = 0f
    private var wheelSpinAngle = 0f
    private var totalPoints = 0f
    private var currentChain = 0f
    private var multiplier = 1
    private var driftComboTimer = 0f
    private var nitroCharge = 100f
    private var camX = 0f
    private var camY = 4.2f
    private var camZ = -10f
    private var lookX = 0f
    private var lookY = 1.2f
    private var lookZ = 0f

    private var lastFrameNanos = System.nanoTime()
    private var elapsedTotal = 0f
    private var hudTimer = 0f
    private var recordTimer = 0f
    private var ghostFrameIdx = 0

    private val recordedFrames = ArrayList<GhostFrame>(1500)
    private val botT = floatArrayOf(0.04f, 0.11f, 0.19f)
    private val botSpeed = floatArrayOf(0.032f, 0.036f, 0.040f)
    private val botLane = floatArrayOf(-3.2f, 3.2f, -3.2f)

    fun resetRaceSession() {
        val cfg = TrackCatalog.get(trackId)
        curve = ClosedCatmullRomCurve3D(cfg.controlPoints)
        val startPt = curve.getPointAt(0f)
        val startTan = curve.getTangentAt(0f)
        val startAngle = atan2(startTan.x, startTan.z)

        carX = startPt.x
        carZ = startPt.z
        heading = startAngle
        velX = 0f
        velZ = 0f
        steerVisual = 0f
        totalPoints = 0f
        currentChain = 0f
        multiplier = 1
        driftComboTimer = 0f
        nitroCharge = 100f
        ghostFrameIdx = 0
        recordedFrames.clear()

        camX = carX - sin(heading) * 9.5f
        camY = 4.2f
        camZ = carZ - cos(heading) * 9.5f
        lookX = carX + sin(heading) * 4f
        lookY = 1.3f
        lookZ = carZ + cos(heading) * 4f

        for (p in smokePositions) {
            p[1] = -999f
            p[3] = 0f
        }
        botT[0] = 0.04f
        botT[1] = 0.11f
        botT[2] = 0.19f
    }

    fun snapshotRecordedFrames(): List<GhostFrame> = synchronized(recordedFrames) {
        recordedFrames.toList()
    }

    fun currentTotalScore(): Int = (totalPoints + currentChain).toInt()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPos = GLES20.glGetAttribLocation(program, "aPosition")
        aNormal = GLES20.glGetAttribLocation(program, "aNormal")
        aColor = GLES20.glGetAttribLocation(program, "aColor")
        uMvp = GLES20.glGetUniformLocation(program, "uMvpMatrix")
        uModel = GLES20.glGetUniformLocation(program, "uModelMatrix")
        uColorTint = GLES20.glGetUniformLocation(program, "uColorTint")
        uFogColor = GLES20.glGetUniformLocation(program, "uFogColor")
        uFogDensity = GLES20.glGetUniformLocation(program, "uFogDensity")
        uEmissive = GLES20.glGetUniformLocation(program, "uEmissive")

        particleCubeMesh = createBoxMesh(1f, 1f, 1f, 0xFFFFFFFF.toInt())
        if (mode == Mode.TURNTABLE) {
            buildTurntableEnvironment()
        } else {
            rebuildRaceEnvironment()
            resetRaceSession()
        }
        rebuildPlayerCar()
        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = if (height > 0) width.toFloat() / height.toFloat() else 1.77f
        val fov = if (mode == Mode.TURNTABLE) 40f else 62f
        val far = if (mode == Mode.TURNTABLE) 80f else 500f
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.25f, far)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastFrameNanos = now
        elapsedTotal += dt

        // Rebuild geometry if user changed track, car style, color, or bot count
        if (builtCarColor != carColorInt || builtCarStyle != carStyle) {
            rebuildPlayerCar()
        }
        if (builtTrackId != trackId || (mode == Mode.RACE && builtBotCount != botCount)) {
            if (mode == Mode.TURNTABLE) {
                buildTurntableEnvironment()
            } else {
                rebuildRaceEnvironment()
                resetRaceSession()
            }
        }

        GLES20.glUseProgram(program)

        if (mode == Mode.TURNTABLE) {
            drawTurntableFrame(dt)
        } else {
            drawRaceFrame(dt)
        }
    }

    private fun drawTurntableFrame(dt: Float) {
        val cfg = TrackCatalog.get(trackId)
        GLES20.glClearColor(0.015f, 0.015f, 0.025f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 3.8f, 2.25f, 5.2f, 0f, 0.55f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        setFog(0x000000, 0.0f)

        // Floor grid
        Matrix.setIdentityM(modelMatrix, 0)
        drawMesh(turntableGridMesh, 0.35f)

        // Turntable disc
        Matrix.setIdentityM(modelMatrix, 0)
        drawMesh(turntablePlatformMesh, 0f)

        // Neon ring
        Matrix.setIdentityM(modelMatrix, 0)
        drawMesh(turntableRingMesh, 0.85f)

        // Rotating car
        val rotDeg = (elapsedTotal * 18f) % 360f
        val bobY = 0.13f + sin(elapsedTotal * 1.8f) * 0.02f
        val rollDeg = sin(elapsedTotal * 1.8f) * 0.6f
        val steerDeg = sin(elapsedTotal * 1.2f) * 16f
        wheelSpinAngle += dt * 90f

        playerRig?.let { rig ->
            drawCarRig(
                rig = rig,
                x = 0f,
                y = bobY,
                z = 0f,
                yawDeg = rotDeg,
                rollDeg = rollDeg,
                steerDeg = steerDeg,
                wheelSpinDeg = wheelSpinAngle,
                tintR = 1f,
                tintG = 1f,
                tintB = 1f
            )
        }

        // Floating accent particles
        val r = ((cfg.accentColor shr 16) and 0xFF) / 255f
        val g = ((cfg.accentColor shr 8) and 0xFF) / 255f
        val b = (cfg.accentColor and 0xFF) / 255f
        for (p in turntableParticles) {
            p[1] += dt * 0.55f
            if (p[1] > 2.8f) {
                p[1] = 0.05f
                p[0] = (Random.nextFloat() - 0.5f) * 4.4f
                p[2] = (Random.nextFloat() - 0.5f) * 4.4f
            }
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p[0], p[1], p[2])
            Matrix.scaleM(modelMatrix, 0, 0.045f, 0.045f, 0.045f)
            drawMesh(particleCubeMesh, 0.9f, r, g, b)
        }
    }

    private fun drawRaceFrame(dt: Float) {
        val cfg = TrackCatalog.get(trackId)
        val skyR = ((cfg.skyColor shr 16) and 0xFF) / 255f
        val skyG = ((cfg.skyColor shr 8) and 0xFF) / 255f
        val skyB = (cfg.skyColor and 0xFF) / 255f
        GLES20.glClearColor(skyR, skyG, skyB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Update Drift Physics
        val throttle = if (canDrive && input.throttle) 1f else 0f
        val brake = if (canDrive && input.brake) 1f else 0f
        val steerLeft = if (canDrive && input.steerLeft) 1f else 0f
        val steerRight = if (canDrive && input.steerRight) 1f else 0f
        val handbrake = canDrive && input.handbrake
        val wantBoost = canDrive && input.boost
        val isBoosting = wantBoost && nitroCharge > 2f && throttle > 0f

        nitroCharge = if (isBoosting) {
            max(0f, nitroCharge - dt * 28f)
        } else {
            min(100f, nitroCharge + dt * 6f)
        }

        val fx = sin(heading)
        val fz = cos(heading)
        val rx = cos(heading)
        val rz = -sin(heading)

        val forwardSpeed = velX * fx + velZ * fz
        val lateralSpeed = velX * rx + velZ * rz
        val totalSpeed = hypot(velX, velZ)

        val steerInput = steerLeft - steerRight
        val dampAlpha = 1f - exp(-10f * dt)
        steerVisual += (steerInput * 0.42f - steerVisual) * dampAlpha

        val speedFactor = min(1f, totalSpeed / 12f)
        val turnRate = (if (handbrake) 2.85f else 2.15f) * (if (forwardSpeed >= -1f) 1f else -1f)
        heading += steerInput * turnRate * speedFactor * dt

        val accel = if (isBoosting) 58f else 36f
        if (throttle > 0f) {
            velX += fx * accel * dt
            velZ += fz * accel * dt
        }
        if (brake > 0f) {
            velX -= fx * 28f * dt
            velZ -= fz * 28f * dt
        }

        val newFx = sin(heading)
        val newFz = cos(heading)
        val newRx = cos(heading)
        val newRz = -sin(heading)

        val fwdComp = velX * newFx + velZ * newFz
        val latComp = velX * newRx + velZ * newRz

        val lateralGrip = if (handbrake) 1.8f else 5.6f
        val dampedLat = latComp * exp(-lateralGrip * dt)
        val drag = if (throttle > 0f || brake > 0f) 0.35f else 1.2f
        val maxSpd = if (isBoosting) 66f else 48f
        val clampedFwd = (fwdComp * exp(-drag * dt)).coerceIn(-18f, maxSpd)

        velX = newFx * clampedFwd + newRx * dampedLat
        velZ = newFz * clampedFwd + newRz * dampedLat
        carX = (carX + velX * dt).coerceIn(-240f, 240f)
        carZ = (carZ + velZ * dt).coerceIn(-240f, 240f)

        val slipAngle = atan2(abs(lateralSpeed), max(2f, abs(forwardSpeed)))
        val isDrifting = canDrive && totalSpeed > 13f && (slipAngle > 0.22f || (handbrake && abs(steerInput) > 0f))

        if (isDrifting) {
            val gain = (totalSpeed * 1.4f + slipAngle * 35f) * dt * multiplier
            currentChain += gain
            driftComboTimer = 1.4f
            multiplier = min(5, 1 + floor(currentChain / 180f).toInt())
            nitroCharge = min(100f, nitroCharge + dt * 14f)

            val slot = smokePositions[smokeCursor]
            slot[0] = carX - newFx * 1.35f + (Random.nextFloat() - 0.5f) * 1.1f
            slot[1] = 0.25f + Random.nextFloat() * 0.2f
            slot[2] = carZ - newFz * 1.35f + (Random.nextFloat() - 0.5f) * 1.1f
            slot[3] = 0.7f
            smokeCursor = (smokeCursor + 1) % smokePoolSize
        } else if (driftComboTimer > 0f) {
            driftComboTimer -= dt
            if (driftComboTimer <= 0f && currentChain > 0f) {
                totalPoints += currentChain
                currentChain = 0f
                multiplier = 1
            }
        }

        // Update smoke particles
        for (s in smokePositions) {
            if (s[3] > 0f) {
                s[1] += dt * 1.1f
                s[3] -= dt
                if (s[3] <= 0f) s[1] = -999f
            }
        }

        // Record ghost frame every 50ms
        recordTimer += dt
        if (canDrive && recordTimer >= 0.05f) {
            recordTimer = 0f
            synchronized(recordedFrames) {
                recordedFrames.add(GhostFrame(carX, 0f, carZ, heading, isDrifting))
            }
        }

        // Update Chase Camera
        val camDist = if (isBoosting) 10.6f else 9.4f
        val targetCamX = carX - newFx * camDist
        val targetCamZ = carZ - newFz * camDist
        val camAlpha = 1f - exp(-7.5f * dt)
        camX += (targetCamX - camX) * camAlpha
        camY += (4.1f - camY) * camAlpha
        camZ += (targetCamZ - camZ) * camAlpha

        val targetLookX = carX + newFx * 4f
        val targetLookZ = carZ + newFz * 4f
        val lookAlpha = 1f - exp(-10f * dt)
        lookX += (targetLookX - lookX) * lookAlpha
        lookY += (1.3f - lookY) * lookAlpha
        lookZ += (targetLookZ - lookZ) * lookAlpha

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        setFog(cfg.fogColor, 0.0042f)

        // Draw world
        Matrix.setIdentityM(modelMatrix, 0)
        drawMesh(groundMesh, 0f)
        drawMesh(curbMesh, 0.05f)
        drawMesh(roadMesh, 0f)
        drawMesh(archMesh, 0.25f)
        drawMesh(polesMesh, 0f)
        drawMesh(bulbsMesh, 0.9f)

        // Draw Player Car
        wheelSpinAngle += forwardSpeed * dt * 110f
        val rollTarget = if (isDrifting) -steerInput * 3.5f else 0f
        val yawDeg = (heading * 180f / PI.toFloat())
        val steerDeg = (steerVisual * 180f / PI.toFloat())
        playerRig?.let { rig ->
            drawCarRig(
                rig = rig,
                x = carX,
                y = 0f,
                z = carZ,
                yawDeg = yawDeg,
                rollDeg = rollTarget,
                steerDeg = steerDeg,
                wheelSpinDeg = wheelSpinAngle,
                tintR = 1f,
                tintG = 1f,
                tintB = 1f
            )
        }

        // Draw AI Bots
        for (i in 0 until min(botCount, botRigs.size)) {
            if (canDrive) {
                botT[i] = (botT[i] + botSpeed[i] * dt) % 1f
            }
            val pt = curve.getPointAt(botT[i])
            val tan = curve.getTangentAt(botT[i])
            val rightX = -tan.z
            val rightZ = tan.x
            val bx = pt.x + rightX * botLane[i]
            val bz = pt.z + rightZ * botLane[i]
            val byaw = atan2(tan.x, tan.z) * 180f / PI.toFloat()
            drawCarRig(
                rig = botRigs[i],
                x = bx,
                y = 0f,
                z = bz,
                yawDeg = byaw,
                rollDeg = 0f,
                steerDeg = 0f,
                wheelSpinDeg = wheelSpinAngle,
                tintR = 1f,
                tintG = 1f,
                tintB = 1f
            )
        }

        // Draw Ghost Replay Car
        val replay = ghostReplay
        if (isGhostActive && replay.isNotEmpty() && ghostRig != null) {
            val gf = replay[ghostFrameIdx % replay.size]
            if (canDrive) ghostFrameIdx++
            drawCarRig(
                rig = ghostRig!!,
                x = gf.x,
                y = gf.y + 0.04f,
                z = gf.z,
                yawDeg = gf.rotationY * 180f / PI.toFloat(),
                rollDeg = 0f,
                steerDeg = 0f,
                wheelSpinDeg = wheelSpinAngle,
                tintR = 0.4f,
                tintG = 0.85f,
                tintB = 1f
            )
        }

        // Draw Tire Smoke Particles
        for (s in smokePositions) {
            if (s[3] > 0f) {
                val scale = 0.45f + (0.7f - s[3]) * 0.9f
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, s[0], s[1], s[2])
                Matrix.scaleM(modelMatrix, 0, scale, scale * 0.7f, scale)
                drawMesh(particleCubeMesh, 0.25f, 0.85f, 0.86f, 0.9f)
            }
        }

        // Audio + HUD Sync
        val kmh = (totalSpeed * 3.4f).toInt()
        audioEngine?.update(kmh.toFloat(), isDrifting, isBoosting)

        hudTimer += dt
        if (hudTimer >= 0.08f) {
            hudTimer = 0f
            onHudUpdate?.invoke(
                DriftHudSnapshot(
                    speedKmh = kmh,
                    totalPoints = totalPoints.toInt(),
                    currentChain = currentChain.toInt(),
                    multiplier = multiplier,
                    nitroCharge = nitroCharge.toInt(),
                    isDrifting = isDrifting
                )
            )
        }
    }

    private fun drawCarRig(
        rig: CarRenderRig,
        x: Float,
        y: Float,
        z: Float,
        yawDeg: Float,
        rollDeg: Float,
        steerDeg: Float,
        wheelSpinDeg: Float,
        tintR: Float,
        tintG: Float,
        tintB: Float
    ) {
        // Chassis
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, rollDeg, 0f, 0f, 1f)
        drawMesh(rig.chassisMesh, 0f, tintR, tintG, tintB)

        // 4 Wheels
        val wheelOffsets = arrayOf(
            floatArrayOf(-rig.wheelHalfTrack, rig.wheelY, rig.frontWheelZ, steerDeg),
            floatArrayOf(rig.wheelHalfTrack, rig.wheelY, rig.frontWheelZ, steerDeg),
            floatArrayOf(-rig.wheelHalfTrack, rig.wheelY, rig.rearWheelZ, 0f),
            floatArrayOf(rig.wheelHalfTrack, rig.wheelY, rig.rearWheelZ, 0f)
        )
        for (w in wheelOffsets) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, x, y, z)
            Matrix.rotateM(modelMatrix, 0, yawDeg, 0f, 1f, 0f)
            Matrix.translateM(modelMatrix, 0, w[0], w[1], w[2])
            if (w[3] != 0f) {
                Matrix.rotateM(modelMatrix, 0, w[3], 0f, 1f, 0f)
            }
            Matrix.rotateM(modelMatrix, 0, wheelSpinDeg, 1f, 0f, 0f)
            drawMesh(rig.wheelMesh, 0f, tintR, tintG, tintB)
        }
    }

    private fun setFog(fogColorHex: Int, density: Float) {
        val r = ((fogColorHex shr 16) and 0xFF) / 255f
        val g = ((fogColorHex shr 8) and 0xFF) / 255f
        val b = (fogColorHex and 0xFF) / 255f
        GLES20.glUniform3f(uFogColor, r, g, b)
        GLES20.glUniform1f(uFogDensity, density)
    }

    private fun drawMesh(
        mesh: MeshBuffer?,
        emissive: Float,
        tintR: Float = 1f,
        tintG: Float = 1f,
        tintB: Float = 1f
    ) {
        if (mesh == null) return
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(uColorTint, tintR, tintG, tintB)
        GLES20.glUniform1f(uEmissive, emissive)
        mesh.draw(aPos, aNormal, aColor)
    }

    private fun rebuildPlayerCar() {
        builtCarColor = carColorInt
        builtCarStyle = carStyle
        playerRig = buildCarRig(carColorInt, carStyle)
        ghostRig = buildCarRig(0xFF38BDF8.toInt(), carStyle)
    }

    private fun buildTurntableEnvironment() {
        val cfg = TrackCatalog.get(trackId)
        builtTrackId = trackId
        turntablePlatformMesh = createCylinderMesh(2.25f, 0.12f, 28, 0x111116)
        turntableRingMesh = createRingMesh(2.18f, 2.26f, 0.132f, 32, cfg.accentColor)
        turntableGridMesh = createGridLinesMesh(14f, 20, cfg.gridColor)
    }

    private fun rebuildRaceEnvironment() {
        val cfg = TrackCatalog.get(trackId)
        builtTrackId = trackId
        builtBotCount = botCount
        curve = ClosedCatmullRomCurve3D(cfg.controlPoints)

        groundMesh = createGroundQuad(480f, cfg.groundColor)
        roadMesh = createRoadRibbon(curve, cfg.roadWidth, 240, 0.04f, cfg.roadColor)
        curbMesh = createCurbRibbon(curve, cfg.roadWidth + 2.4f, 240, 0.025f, cfg.curbColorA)
        archMesh = createStartArch(curve, cfg.roadWidth, cfg.accentColor)

        val (poles, bulbs) = createTrackPoles(curve, cfg.roadWidth, 44, cfg.accentColor)
        polesMesh = poles
        bulbsMesh = bulbs

        val botColors = listOf(0xFF3B82F6.toInt(), 0xFFEAB308.toInt(), 0xFF22C55E.toInt())
        val botStyles = listOf(CarStyle.JDM, CarStyle.MUSCLE, CarStyle.SUPER)
        botRigs = (0 until 3).map { i ->
            buildCarRig(botColors[i], botStyles[i])
        }
    }

    private fun buildCarRig(paintColor: Int, style: CarStyle): CarRenderRig {
        val builder = GeometryBuilder()
        val darkTrim = 0x18181D
        val glass = 0x0A121C
        val headlight = 0xFEF08A
        val taillight = 0xEF4444
        val rim = 0xD4D4D8

        if (style == CarStyle.PLANE) {
            // Fuselage, nose, canopy, wings, tail fin, thruster
            builder.addBox(0f, 0.65f, 0f, 0.72f, 0.62f, 3.5f, paintColor)
            builder.addBox(0f, 0.62f, 2.05f, 0.46f, 0.42f, 0.75f, darkTrim)
            builder.addBox(0f, 1.02f, 0.35f, 0.46f, 0.32f, 1.15f, glass)
            builder.addBox(0f, 0.64f, 0.1f, 3.85f, 0.1f, 0.95f, paintColor)
            builder.addBox(0f, 0.72f, -1.55f, 1.55f, 0.08f, 0.45f, darkTrim)
            builder.addBox(0f, 1.08f, -1.5f, 0.1f, 0.72f, 0.65f, paintColor)
            builder.addBox(0f, 0.65f, -1.85f, 0.4f, 0.38f, 0.25f, taillight)

            val wheelB = GeometryBuilder()
            wheelB.addBox(0f, 0f, 0f, 0.16f, 0.42f, 0.42f, 0x111114)
            return CarRenderRig(
                chassisMesh = builder.build(),
                wheelMesh = wheelB.build(),
                frontWheelZ = 1.1f,
                rearWheelZ = -1.1f,
                wheelHalfTrack = 0.62f,
                wheelY = 0.22f
            )
        }

        val isMuscle = style == CarStyle.MUSCLE
        val isSuper = style == CarStyle.SUPER
        val isJdm = style == CarStyle.JDM

        val bodyW = if (isSuper) 1.88f else 1.76f
        val bodyH = if (isSuper) 0.42f else if (isMuscle) 0.56f else 0.50f
        val bodyL = if (isMuscle) 4.25f else 4.0f

        // Lower splitter
        builder.addBox(0f, 0.26f, 0f, bodyW + 0.08f, 0.16f, bodyL + 0.16f, darkTrim)
        // Main body
        builder.addBox(0f, 0.26f + bodyH / 2f + 0.05f, 0f, bodyW, bodyH, bodyL, paintColor)
        // Hood bulge
        builder.addBox(
            0f,
            0.26f + bodyH + 0.07f,
            bodyL * 0.22f,
            bodyW * 0.52f,
            0.1f,
            bodyL * 0.34f,
            if (isMuscle) darkTrim else paintColor
        )
        // Cabin & roof
        val cabinL = if (isSuper) 1.65f else 1.85f
        val cabinH = if (isSuper) 0.34f else 0.42f
        val cabinZ = if (isSuper) -0.05f else -0.18f
        builder.addBox(0f, 0.26f + bodyH + cabinH / 2f + 0.03f, cabinZ, bodyW * 0.82f, cabinH, cabinL, glass)
        builder.addBox(0f, 0.26f + bodyH + cabinH + 0.04f, cabinZ - 0.04f, bodyW * 0.76f, 0.06f, cabinL * 0.65f, paintColor)

        // Rear Spoiler (JDM, Super, Coupe)
        if (!isMuscle) {
            val wingH = if (isJdm) 0.32f else if (isSuper) 0.28f else 0.18f
            builder.addBox(-bodyW * 0.32f, 0.26f + bodyH + wingH / 2f, -bodyL * 0.42f, 0.07f, wingH, 0.14f, darkTrim)
            builder.addBox(bodyW * 0.32f, 0.26f + bodyH + wingH / 2f, -bodyL * 0.42f, 0.07f, wingH, 0.14f, darkTrim)
            builder.addBox(
                0f,
                0.26f + bodyH + wingH + 0.02f,
                -bodyL * 0.44f,
                bodyW * 0.96f,
                0.05f,
                0.28f,
                if (isJdm) darkTrim else paintColor
            )
        }

        // Headlights & Taillights
        builder.addBox(-bodyW * 0.32f, 0.52f, bodyL / 2f + 0.02f, 0.36f, 0.12f, 0.08f, headlight)
        builder.addBox(bodyW * 0.32f, 0.52f, bodyL / 2f + 0.02f, 0.36f, 0.12f, 0.08f, headlight)
        builder.addBox(0f, 0.56f, -bodyL / 2f - 0.02f, bodyW * 0.84f, 0.1f, 0.06f, taillight)

        // Exhausts
        builder.addBox(-0.42f, 0.24f, -bodyL / 2f - 0.06f, 0.12f, 0.12f, 0.16f, rim)
        builder.addBox(0.42f, 0.24f, -bodyL / 2f - 0.06f, 0.12f, 0.12f, 0.16f, rim)

        // Wheel mesh (tire + rim hub)
        val wheelB = GeometryBuilder()
        wheelB.addWheel(0.34f, 0.24f, 0x111114, rim)

        return CarRenderRig(
            chassisMesh = builder.build(),
            wheelMesh = wheelB.build(),
            frontWheelZ = bodyL * 0.32f,
            rearWheelZ = -bodyL * 0.32f,
            wheelHalfTrack = bodyW / 2f + 0.03f,
            wheelY = 0.34f
        )
    }

    // --- Geometry Generators ---

    private fun createBoxMesh(w: Float, h: Float, d: Float, color: Int): MeshBuffer {
        val gb = GeometryBuilder()
        gb.addBox(0f, 0f, 0f, w, h, d, color)
        return gb.build()
    }

    private fun createCylinderMesh(radius: Float, height: Float, segs: Int, color: Int): MeshBuffer {
        val gb = GeometryBuilder()
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
            val x0 = cos(a0) * radius
            val z0 = sin(a0) * radius
            val x1 = cos(a1) * radius
            val z1 = sin(a1) * radius
            // Top cap (CCW looking from +Y)
            gb.tri(0f, height, 0f, x1, height, z1, x0, height, z0, 0f, 1f, 0f, r, g, b)
            // Side quad
            val nx = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)
            gb.quad(
                x0, 0f, z0,
                x0, height, z0,
                x1, height, z1,
                x1, 0f, z1,
                nx, 0f, nz,
                r * 0.7f, g * 0.7f, b * 0.7f
            )
        }
        return gb.build()
    }

    private fun createRingMesh(innerR: Float, outerR: Float, y: Float, segs: Int, color: Int): MeshBuffer {
        val gb = GeometryBuilder()
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
            val c0 = cos(a0)
            val s0 = sin(a0)
            val c1 = cos(a1)
            val s1 = sin(a1)
            gb.quad(
                c0 * innerR, y, s0 * innerR,
                c1 * innerR, y, s1 * innerR,
                c1 * outerR, y, s1 * outerR,
                c0 * outerR, y, s0 * outerR,
                0f, 1f, 0f,
                r, g, b
            )
        }
        return gb.build()
    }

    private fun createGridLinesMesh(size: Float, divisions: Int, color: Int): MeshBuffer {
        val half = size / 2f
        val step = size / divisions
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val floats = ArrayList<Float>((divisions + 1) * 4 * 9)
        fun addVert(x: Float, y: Float, z: Float) {
            floats.add(x); floats.add(y); floats.add(z)
            floats.add(0f); floats.add(1f); floats.add(0f)
            floats.add(r); floats.add(g); floats.add(b)
        }
        for (i in 0..divisions) {
            val p = -half + i * step
            addVert(-half, 0.01f, p); addVert(half, 0.01f, p)
            addVert(p, 0.01f, -half); addVert(p, 0.01f, half)
        }
        val arr = floats.toFloatArray()
        val buf = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(arr).position(0)
        return MeshBuffer(buf, arr.size / 9, GLES20.GL_LINES)
    }

    private fun createGroundQuad(halfSize: Float, color: Int): MeshBuffer {
        val gb = GeometryBuilder()
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        gb.quad(
            -halfSize, 0f, -halfSize,
            -halfSize, 0f, halfSize,
            halfSize, 0f, halfSize,
            halfSize, 0f, -halfSize,
            0f, 1f, 0f,
            r, g, b
        )
        return gb.build()
    }

    private fun createRoadRibbon(
        curve: ClosedCatmullRomCurve3D,
        width: Float,
        segments: Int,
        yOffset: Float,
        roadColor: Int
    ): MeshBuffer {
        val gb = GeometryBuilder()
        val rr = ((roadColor shr 16) and 0xFF) / 255f
        val rg = ((roadColor shr 8) and 0xFF) / 255f
        val rb = (roadColor and 0xFF) / 255f
        val halfW = width / 2f

        for (i in 0 until segments) {
            val t0 = i.toFloat() / segments
            val t1 = ((i + 1) % segments).toFloat() / segments
            val p0 = curve.getPointAt(t0)
            val tan0 = curve.getTangentAt(t0)
            val right0 = Vec3(-tan0.z, 0f, tan0.x).normalized()

            val p1 = curve.getPointAt(t1)
            val tan1 = curve.getTangentAt(t1)
            val right1 = Vec3(-tan1.z, 0f, tan1.x).normalized()

            val l0 = p0 - right0 * halfW
            val r0 = p0 + right0 * halfW
            val l1 = p1 - right1 * halfW
            val r1 = p1 + right1 * halfW

            // Subtle asphalt stripe variation
            val shade = if (i % 2 == 0) 1.0f else 0.92f
            gb.quad(
                l0.x, yOffset, l0.z,
                r0.x, yOffset, r0.z,
                r1.x, yOffset, r1.z,
                l1.x, yOffset, l1.z,
                0f, 1f, 0f,
                rr * shade, rg * shade, rb * shade
            )

            // Dashed center line
            if (i % 3 != 0) {
                val cl0 = p0 - right0 * 0.28f
                val cr0 = p0 + right0 * 0.28f
                val cl1 = p1 - right1 * 0.28f
                val cr1 = p1 + right1 * 0.28f
                gb.quad(
                    cl0.x, yOffset + 0.015f, cl0.z,
                    cr0.x, yOffset + 0.015f, cr0.z,
                    cr1.x, yOffset + 0.015f, cr1.z,
                    cl1.x, yOffset + 0.015f, cl1.z,
                    0f, 1f, 0f,
                    0.85f, 0.85f, 0.88f
                )
            }
        }
        return gb.build()
    }

    private fun createCurbRibbon(
        curve: ClosedCatmullRomCurve3D,
        width: Float,
        segments: Int,
        yOffset: Float,
        curbColorA: Int
    ): MeshBuffer {
        val gb = GeometryBuilder()
        val cr = ((curbColorA shr 16) and 0xFF) / 255f
        val cg = ((curbColorA shr 8) and 0xFF) / 255f
        val cb = (curbColorA and 0xFF) / 255f
        val halfW = width / 2f

        for (i in 0 until segments) {
            val t0 = i.toFloat() / segments
            val t1 = ((i + 1) % segments).toFloat() / segments
            val p0 = curve.getPointAt(t0)
            val tan0 = curve.getTangentAt(t0)
            val right0 = Vec3(-tan0.z, 0f, tan0.x).normalized()

            val p1 = curve.getPointAt(t1)
            val tan1 = curve.getTangentAt(t1)
            val right1 = Vec3(-tan1.z, 0f, tan1.x).normalized()

            val l0 = p0 - right0 * halfW
            val r0 = p0 + right0 * halfW
            val l1 = p1 - right1 * halfW
            val r1 = p1 + right1 * halfW

            val isAlt = i % 2 == 0
            gb.quad(
                l0.x, yOffset, l0.z,
                r0.x, yOffset, r0.z,
                r1.x, yOffset, r1.z,
                l1.x, yOffset, l1.z,
                0f, 1f, 0f,
                if (isAlt) cr else 0.88f,
                if (isAlt) cg else 0.88f,
                if (isAlt) cb else 0.90f
            )
        }
        return gb.build()
    }

    private fun createStartArch(
        curve: ClosedCatmullRomCurve3D,
        roadWidth: Float,
        accentColor: Int
    ): MeshBuffer {
        val gb = GeometryBuilder()
        val pt = curve.getPointAt(0f)
        val tan = curve.getTangentAt(0f)
        val right = Vec3(-tan.z, 0f, tan.x).normalized()
        val offset = roadWidth / 2f + 0.9f

        val leftPos = pt - right * offset
        val rightPos = pt + right * offset
        gb.addBox(leftPos.x, 3.25f, leftPos.z, 1.0f, 6.5f, 1.0f, 0x18181B)
        gb.addBox(rightPos.x, 3.25f, rightPos.z, 1.0f, 6.5f, 1.0f, 0x18181B)

        val yaw = atan2(tan.x, tan.z)
        gb.addRotatedBox(pt.x, 6.2f, pt.z, roadWidth + 2.6f, 1.3f, 0.7f, yaw, accentColor)
        return gb.build()
    }

    private fun createTrackPoles(
        curve: ClosedCatmullRomCurve3D,
        roadWidth: Float,
        count: Int,
        accentColor: Int
    ): Pair<MeshBuffer, MeshBuffer> {
        val polesB = GeometryBuilder()
        val bulbsB = GeometryBuilder()
        val offset = roadWidth / 2f + 3.2f
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val pt = curve.getPointAt(t)
            val tan = curve.getTangentAt(t)
            val right = Vec3(-tan.z, 0f, tan.x).normalized()
            val side = if (i % 2 == 0) 1f else -1f
            val px = pt.x + right.x * side * offset
            val pz = pt.z + right.z * side * offset
            polesB.addBox(px, 2.75f, pz, 0.35f, 5.5f, 0.35f, 0x27272A)
            bulbsB.addBox(px, 5.65f, pz, 0.65f, 0.3f, 0.65f, accentColor)
        }
        return polesB.build() to bulbsB.build()
    }

    private class GeometryBuilder {
        private val floats = ArrayList<Float>(1024)

        fun tri(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            nx: Float, ny: Float, nz: Float,
            r: Float, g: Float, b: Float
        ) {
            push(x0, y0, z0, nx, ny, nz, r, g, b)
            push(x1, y1, z1, nx, ny, nz, r, g, b)
            push(x2, y2, z2, nx, ny, nz, r, g, b)
        }

        fun quad(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float,
            nx: Float, ny: Float, nz: Float,
            r: Float, g: Float, b: Float
        ) {
            tri(x0, y0, z0, x1, y1, z1, x2, y2, z2, nx, ny, nz, r, g, b)
            tri(x0, y0, z0, x2, y2, z2, x3, y3, z3, nx, ny, nz, r, g, b)
        }

        private fun push(
            x: Float, y: Float, z: Float,
            nx: Float, ny: Float, nz: Float,
            r: Float, g: Float, b: Float
        ) {
            floats.add(x); floats.add(y); floats.add(z)
            floats.add(nx); floats.add(ny); floats.add(nz)
            floats.add(r); floats.add(g); floats.add(b)
        }

        fun addBox(cx: Float, cy: Float, cz: Float, w: Float, h: Float, d: Float, color: Int) {
            addRotatedBox(cx, cy, cz, w, h, d, 0f, color)
        }

        fun addRotatedBox(
            cx: Float, cy: Float, cz: Float,
            w: Float, h: Float, d: Float,
            yawRad: Float,
            color: Int
        ) {
            val hw = w / 2f
            val hh = h / 2f
            val hd = d / 2f
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            val cosY = cos(yawRad)
            val sinY = sin(yawRad)

            fun tx(lx: Float, lz: Float) = cx + lx * cosY + lz * sinY
            fun tz(lx: Float, lz: Float) = cz - lx * sinY + lz * cosY
            fun nrx(lx: Float, lz: Float) = lx * cosY + lz * sinY
            fun nrz(lx: Float, lz: Float) = -lx * sinY + lz * cosY

            // Front (+Z)
            quad(
                tx(-hw, hd), cy - hh, tz(-hw, hd),
                tx(hw, hd), cy - hh, tz(hw, hd),
                tx(hw, hd), cy + hh, tz(hw, hd),
                tx(-hw, hd), cy + hh, tz(-hw, hd),
                nrx(0f, 1f), 0f, nrz(0f, 1f), r, g, b
            )
            // Back (-Z)
            quad(
                tx(hw, -hd), cy - hh, tz(hw, -hd),
                tx(-hw, -hd), cy - hh, tz(-hw, -hd),
                tx(-hw, -hd), cy + hh, tz(-hw, -hd),
                tx(hw, -hd), cy + hh, tz(hw, -hd),
                nrx(0f, -1f), 0f, nrz(0f, -1f), r, g, b
            )
            // Left (-X)
            quad(
                tx(-hw, -hd), cy - hh, tz(-hw, -hd),
                tx(-hw, hd), cy - hh, tz(-hw, hd),
                tx(-hw, hd), cy + hh, tz(-hw, hd),
                tx(-hw, -hd), cy + hh, tz(-hw, -hd),
                nrx(-1f, 0f), 0f, nrz(-1f, 0f), r * 0.9f, g * 0.9f, b * 0.9f
            )
            // Right (+X)
            quad(
                tx(hw, hd), cy - hh, tz(hw, hd),
                tx(hw, -hd), cy - hh, tz(hw, -hd),
                tx(hw, -hd), cy + hh, tz(hw, -hd),
                tx(hw, hd), cy + hh, tz(hw, hd),
                nrx(1f, 0f), 0f, nrz(1f, 0f), r * 0.9f, g * 0.9f, b * 0.9f
            )
            // Top (+Y)
            quad(
                tx(-hw, hd), cy + hh, tz(-hw, hd),
                tx(hw, hd), cy + hh, tz(hw, hd),
                tx(hw, -hd), cy + hh, tz(hw, -hd),
                tx(-hw, -hd), cy + hh, tz(-hw, -hd),
                0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f
            )
            // Bottom (-Y)
            quad(
                tx(-hw, -hd), cy - hh, tz(-hw, -hd),
                tx(hw, -hd), cy - hh, tz(hw, -hd),
                tx(hw, hd), cy - hh, tz(hw, hd),
                tx(-hw, hd), cy - hh, tz(-hw, hd),
                0f, -1f, 0f, r * 0.6f, g * 0.6f, b * 0.6f
            )
        }

        fun addWheel(radius: Float, width: Float, tireColor: Int, rimColor: Int) {
            val segs = 14
            val hw = width / 2f
            val tr = ((tireColor shr 16) and 0xFF) / 255f
            val tg = ((tireColor shr 8) and 0xFF) / 255f
            val tb = (tireColor and 0xFF) / 255f
            val rr = ((rimColor shr 16) and 0xFF) / 255f
            val rg = ((rimColor shr 8) and 0xFF) / 255f
            val rb = (rimColor and 0xFF) / 255f

            for (i in 0 until segs) {
                val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
                val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
                val y0 = cos(a0) * radius
                val z0 = sin(a0) * radius
                val y1 = cos(a1) * radius
                val z1 = sin(a1) * radius
                val ny = cos((a0 + a1) * 0.5f)
                val nz = sin((a0 + a1) * 0.5f)

                // Tire tread
                quad(
                    -hw, y0, z0,
                    hw, y0, z0,
                    hw, y1, z1,
                    -hw, y1, z1,
                    0f, ny, nz,
                    tr, tg, tb
                )
                // Side caps (tire + inner rim)
                tri(-hw, 0f, 0f, -hw, y0, z0, -hw, y1, z1, -1f, 0f, 0f, tr, tg, tb)
                tri(hw, 0f, 0f, hw, y1, z1, hw, y0, z0, 1f, 0f, 0f, tr, tg, tb)

                val ir = radius * 0.6f
                tri(-hw - 0.01f, 0f, 0f, -hw - 0.01f, y0 * 0.6f, z0 * 0.6f, -hw - 0.01f, y1 * 0.6f, z1 * 0.6f, -1f, 0f, 0f, rr, rg, rb)
                tri(hw + 0.01f, 0f, 0f, hw + 0.01f, y1 * 0.6f, z1 * 0.6f, hw + 0.01f, y0 * 0.6f, z0 * 0.6f, 1f, 0f, 0f, rr, rg, rb)
            }
        }

        fun build(): MeshBuffer {
            val arr = floats.toFloatArray()
            val buf = ByteBuffer.allocateDirect(arr.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
            buf.put(arr).position(0)
            return MeshBuffer(buf, arr.size / 9, GLES20.GL_TRIANGLES)
        }
    }

    private fun buildProgram(vs: String, fs: String): Int {
        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vs)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fs)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vShader)
        GLES20.glAttachShader(prog, fShader)
        GLES20.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMvpMatrix;
            uniform mat4 uModelMatrix;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            attribute vec3 aColor;
            varying vec3 vColor;
            varying vec3 vNormal;
            varying float vViewDist;
            void main() {
                vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
                vec4 clipPos = uMvpMatrix * vec4(aPosition, 1.0);
                gl_Position = clipPos;
                vColor = aColor;
                vNormal = normalize(mat3(uModelMatrix) * aNormal);
                vViewDist = clipPos.w;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec3 uColorTint;
            uniform vec3 uFogColor;
            uniform float uFogDensity;
            uniform float uEmissive;
            varying vec3 vColor;
            varying vec3 vNormal;
            varying float vViewDist;
            void main() {
                vec3 sunDir = normalize(vec3(0.45, 0.85, 0.35));
                float diff = max(dot(normalize(vNormal), sunDir), 0.0);
                float hemi = 0.42 + 0.18 * vNormal.y;
                vec3 litColor = vColor * uColorTint * (hemi + diff * 0.72 + uEmissive);
                float fogFactor = 1.0 - exp(-uFogDensity * uFogDensity * vViewDist * vViewDist);
                fogFactor = clamp(fogFactor, 0.0, 1.0);
                vec3 finalColor = mix(litColor, uFogColor, fogFactor);
                gl_FragColor = vec4(finalColor, 1.0);
            }
        """
    }
}
