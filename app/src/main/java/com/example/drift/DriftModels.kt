package com.example.drift

import kotlin.math.floor
import kotlin.math.sqrt

enum class TrackId(val key: String) {
    JAPAN("japan"),
    USA("usa"),
    BRAZIL("brazil"),
    SCRAPYARD("scrapyard"),
    AIRPORT("airport")
}

enum class CarStyle(val label: String) {
    COUPE("Coupé GT"),
    JDM("JDM Drift"),
    MUSCLE("Muscle V8"),
    SUPER("Supercarro"),
    PLANE("Avião Secreto")
}

data class CarColorOption(
    val name: String,
    val hex: String,
    val colorInt: Int
)

val DRIFT_CAR_COLORS = listOf(
    CarColorOption("Vermelho Corsa", "#E62424", 0xFFE62424.toInt()),
    CarColorOption("Azul Nitro", "#2563EB", 0xFF2563EB.toInt()),
    CarColorOption("Verde Tóquio", "#16A34A", 0xFF16A34A.toInt()),
    CarColorOption("Amarelo Solar", "#EAB308", 0xFFEAB308.toInt()),
    CarColorOption("Branco Pérola", "#F4F4F5", 0xFFF4F4F5.toInt()),
    CarColorOption("Roxo Midnight", "#9333EA", 0xFF9333EA.toInt())
)

data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun normalized(): Vec3 {
        val len = sqrt(x * x + y * y + z * z)
        return if (len > 1e-6f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 1f)
    }
}

data class TrackConfig(
    val id: TrackId,
    val name: String,
    val country: String,
    val description: String,
    val difficulty: String,
    val skyColor: Int,
    val fogColor: Int,
    val groundColor: Int,
    val roadColor: Int,
    val curbColorA: Int,
    val accentColor: Int,
    val gridColor: Int,
    val roadWidth: Float,
    val controlPoints: List<Vec3>
)

object TrackCatalog {
    val tracks: Map<TrackId, TrackConfig> = mapOf(
        TrackId.JAPAN to TrackConfig(
            id = TrackId.JAPAN,
            name = "Tokyo Touge",
            country = "Japão",
            description = "Curvas técnicas noturnas com iluminação néon vermelha e asfalto liso.",
            difficulty = "Fácil",
            skyColor = 0x07080D,
            fogColor = 0x090B12,
            groundColor = 0x101318,
            roadColor = 0x1C1E24,
            curbColorA = 0xEF4444,
            accentColor = 0xEF4444,
            gridColor = 0xFFFFFF,
            roadWidth = 18f,
            controlPoints = listOf(
                Vec3(0f, 0f, -90f),
                Vec3(75f, 0f, -85f),
                Vec3(115f, 0f, -25f),
                Vec3(85f, 0f, 45f),
                Vec3(110f, 0f, 105f),
                Vec3(30f, 0f, 125f),
                Vec3(-55f, 0f, 95f),
                Vec3(-105f, 0f, 35f),
                Vec3(-75f, 0f, -35f),
                Vec3(-95f, 0f, -80f)
            )
        ),
        TrackId.USA to TrackConfig(
            id = TrackId.USA,
            name = "Nevada Speedbowl",
            country = "EUA",
            description = "Retas largas para velocidade máxima e curvas amplas de alta velocidade.",
            difficulty = "Médio",
            skyColor = 0x0C101A,
            fogColor = 0x101624,
            groundColor = 0x1C1713,
            roadColor = 0x22252C,
            curbColorA = 0x3B82F6,
            accentColor = 0x3B82F6,
            gridColor = 0x2563EB,
            roadWidth = 20f,
            controlPoints = listOf(
                Vec3(0f, 0f, -110f),
                Vec3(110f, 0f, -100f),
                Vec3(145f, 0f, -10f),
                Vec3(115f, 0f, 90f),
                Vec3(0f, 0f, 115f),
                Vec3(-115f, 0f, 90f),
                Vec3(-140f, 0f, 0f),
                Vec3(-105f, 0f, -95f)
            )
        ),
        TrackId.BRAZIL to TrackConfig(
            id = TrackId.BRAZIL,
            name = "Circuito Paulista",
            country = "Brasil",
            description = "Sequência de S em alta rotação exigindo trocas rápidas de ângulo no drift.",
            difficulty = "Médio",
            skyColor = 0x08130E,
            fogColor = 0x0A1811,
            groundColor = 0x102016,
            roadColor = 0x1E2124,
            curbColorA = 0x22C55E,
            accentColor = 0x22C55E,
            gridColor = 0x22C55E,
            roadWidth = 17f,
            controlPoints = listOf(
                Vec3(0f, 0f, -105f),
                Vec3(85f, 0f, -95f),
                Vec3(120f, 0f, -35f),
                Vec3(60f, 0f, 5f),
                Vec3(110f, 0f, 65f),
                Vec3(55f, 0f, 120f),
                Vec3(-35f, 0f, 95f),
                Vec3(-85f, 0f, 125f),
                Vec3(-125f, 0f, 40f),
                Vec3(-70f, 0f, -25f),
                Vec3(-110f, 0f, -85f)
            )
        ),
        TrackId.SCRAPYARD to TrackConfig(
            id = TrackId.SCRAPYARD,
            name = "Trilha da Floresta",
            country = "Especial",
            description = "Traçado estreito entre pinheiros iluminados com curvas fechadas.",
            difficulty = "Difícil",
            skyColor = 0x040D11,
            fogColor = 0x061318,
            groundColor = 0x0B1914,
            roadColor = 0x181B1E,
            curbColorA = 0x0EA5E9,
            accentColor = 0x0EA5E9,
            gridColor = 0x0EA5E9,
            roadWidth = 15f,
            controlPoints = listOf(
                Vec3(0f, 0f, -100f),
                Vec3(60f, 0f, -115f),
                Vec3(125f, 0f, -65f),
                Vec3(90f, 0f, 0f),
                Vec3(130f, 0f, 75f),
                Vec3(45f, 0f, 110f),
                Vec3(0f, 0f, 55f),
                Vec3(-55f, 0f, 115f),
                Vec3(-125f, 0f, 65f),
                Vec3(-90f, 0f, -10f),
                Vec3(-120f, 0f, -75f)
            )
        ),
        TrackId.AIRPORT to TrackConfig(
            id = TrackId.AIRPORT,
            name = "Pista Secreta",
            country = "Aviação",
            description = "Aeródromo clandestino com retas gigantescas e curvas de pista de pouso.",
            difficulty = "Difícil",
            skyColor = 0x121009,
            fogColor = 0x16130A,
            groundColor = 0x161512,
            roadColor = 0x202124,
            curbColorA = 0xEAB308,
            accentColor = 0xFACC15,
            gridColor = 0xCA8A04,
            roadWidth = 24f,
            controlPoints = listOf(
                Vec3(0f, 0f, -135f),
                Vec3(140f, 0f, -135f),
                Vec3(165f, 0f, -40f),
                Vec3(140f, 0f, 115f),
                Vec3(0f, 0f, 135f),
                Vec3(-140f, 0f, 115f),
                Vec3(-165f, 0f, -40f),
                Vec3(-140f, 0f, -135f)
            )
        )
    )

    fun get(trackId: TrackId): TrackConfig = tracks[trackId] ?: tracks.getValue(TrackId.JAPAN)
}

class ClosedCatmullRomCurve3D(private val points: List<Vec3>) {
    private val n = points.size

    fun getPointAt(tNorm: Float): Vec3 {
        val wrapped = ((tNorm % 1f) + 1f) % 1f
        val p = wrapped * n
        val intPoint = floor(p).toInt()
        val weight = p - intPoint

        val p0 = points[(intPoint - 1 + n) % n]
        val p1 = points[intPoint % n]
        val p2 = points[(intPoint + 1) % n]
        val p3 = points[(intPoint + 2) % n]

        return Vec3(
            catmullRom(weight, p0.x, p1.x, p2.x, p3.x),
            catmullRom(weight, p0.y, p1.y, p2.y, p3.y),
            catmullRom(weight, p0.z, p1.z, p2.z, p3.z)
        )
    }

    fun getTangentAt(tNorm: Float): Vec3 {
        val delta = 0.001f
        val a = getPointAt(tNorm - delta)
        val b = getPointAt(tNorm + delta)
        return (b - a).normalized()
    }

    private fun catmullRom(t: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
        val v0 = (p2 - p0) * 0.5f
        val v1 = (p3 - p1) * 0.5f
        val t2 = t * t
        val t3 = t * t2
        return (2f * p1 - 2f * p2 + v0 + v1) * t3 +
            (-3f * p1 + 3f * p2 - 2f * v0 - v1) * t2 +
            v0 * t +
            p1
    }
}

data class GhostFrame(
    val x: Float,
    val y: Float,
    val z: Float,
    val rotationY: Float,
    val isDrifting: Boolean
)

enum class DriftRaceStatus {
    LOBBY,
    COUNTDOWN,
    RACING,
    FINISHED
}

data class DriftHudSnapshot(
    val speedKmh: Int = 0,
    val totalPoints: Int = 0,
    val currentChain: Int = 0,
    val multiplier: Int = 1,
    val nitroCharge: Int = 100,
    val isDrifting: Boolean = false
)

class DriftControlInput {
    @Volatile var throttle: Boolean = false
    @Volatile var brake: Boolean = false
    @Volatile var steerLeft: Boolean = false
    @Volatile var steerRight: Boolean = false
    @Volatile var handbrake: Boolean = false
    @Volatile var boost: Boolean = false

    fun reset() {
        throttle = false
        brake = false
        steerLeft = false
        steerRight = false
        handbrake = false
        boost = false
    }
}
