package com.example.royale.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.royale.FriendEntry
import com.example.royale.RoyaleCardCatalog
import com.example.royale.data.RoyaleProfileState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun RoyaleClanFriendsTab(
    profile: RoyaleProfileState,
    clanDirectory: List<ClanDirectoryEntry>,
    onSendChat: (String) -> Unit,
    onRequestDonation: (String) -> Unit,
    onDonateToMsg: (String) -> Unit,
    onCreateClan: (String, String, String, Int) -> Unit,
    onJoinClan: (ClanDirectoryEntry) -> Unit,
    onLeaveClan: () -> Unit,
    onAddFriend: (String) -> Unit,
    onAcceptFriend: (String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onChallengeFriend: (FriendEntry) -> Unit
) {
    var topSegment by remember { mutableIntStateOf(0) } // 0 = Clã, 1 = Amigos
    var clanSubTab by remember { mutableIntStateOf(0) } // 0 = Chat & Doações, 1 = Membros, 2 = Buscar/Criar
    var chatInput by remember { mutableStateOf("") }
    var showDonationPicker by remember { mutableStateOf(false) }
    var showCreateClanDialog by remember { mutableStateOf(false) }
    var friendQuery by remember { mutableStateOf("") }
    var selectedFriendProfile by remember { mutableStateOf<FriendEntry?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Toggle: Clã vs Amigos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SegmentButton(
                label = "CLÃ",
                selected = topSegment == 0,
                onClick = { topSegment = 0 },
                modifier = Modifier.weight(1f).testTag("social_clan_segment")
            )
            SegmentButton(
                label = "AMIGOS (${profile.friends.size})",
                selected = topSegment == 1,
                onClick = { topSegment = 1 },
                modifier = Modifier.weight(1f).testTag("social_friends_segment")
            )
        }

        if (topSegment == 0) {
            if (!profile.inClan) {
                ClanDirectoryView(
                    directory = clanDirectory,
                    onJoinClan = onJoinClan,
                    onOpenCreate = { showCreateClanDialog = true }
                )
            } else {
                // Clan Header + Subtabs
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Text(profile.clanEmblem, fontSize = 28.sp)
                            Column {
                                Text("${profile.clanName} ${profile.clanTag}", color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Black, fontSize = 15.sp)
                                Text(profile.clanDescription, color = Color(0xFF94A3B8), fontFamily = SpaceGroteskFamily, fontSize = 11.sp, maxLines = 2)
                            }
                        }
                        IconButton(onClick = onLeaveClan) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair do clã", tint = Color(0xFFF43F5E))
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SegmentButton("Chat & Doações", clanSubTab == 0, { clanSubTab = 0 }, Modifier.weight(1f))
                    SegmentButton("Membros", clanSubTab == 1, { clanSubTab = 1 }, Modifier.weight(1f))
                    SegmentButton("Explorar Clãs", clanSubTab == 2, { clanSubTab = 2 }, Modifier.weight(1f))
                }

                when (clanSubTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(profile.clanChat) { msg ->
                                Surface(
                                    color = if (msg.isDonationRequest) Color(0xFF1E1B4B) else Color(0xFF0F172A),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (msg.isDonationRequest) Color(0xFFA855F7) else Color(0xFF1E293B))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(msg.sender, color = Color(0xFF38BDF8), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            if (msg.isDonationRequest) {
                                                Text("${msg.donatedCount}/${msg.maxDonation} doadas", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                                            }
                                        }
                                        Text(msg.text, color = Color.White, fontFamily = SpaceGroteskFamily, fontSize = 13.sp)
                                        if (msg.isDonationRequest && msg.donatedCount < msg.maxDonation && msg.sender != profile.playerName) {
                                            Button(
                                                onClick = { onDonateToMsg(msg.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text("DOAR 1 CARTA (+15🪙)", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Chat Input + Request Card button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDonationPicker = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Filled.CardGiftcard, contentDescription = "Pedir carta", tint = Color(0xFFFACC15), modifier = Modifier.size(18.dp))
                            }
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text("Mensagem para o clã...", color = Color(0xFF64748B), fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        onSendChat(chatInput)
                                        chatInput = ""
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color(0xFF38BDF8))
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(profile.clanMembers.sortedByDescending { it.trophies }) { member ->
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(member.name, color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${member.role} • Nv.${member.level} • Doações: ${member.donations}", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                                        }
                                        Text("🏆 ${member.trophies}", color = Color(0xFFFACC15), fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        ClanDirectoryView(
                            directory = clanDirectory,
                            onJoinClan = onJoinClan,
                            onOpenCreate = { showCreateClanDialog = true }
                        )
                    }
                }
            }
        } else {
            // FRIENDS VIEW
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = friendQuery,
                            onValueChange = { friendQuery = it },
                            placeholder = { Text("Adicionar amigo pelo nome...", color = Color(0xFF64748B), fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (friendQuery.isNotBlank()) {
                                    onAddFriend(friendQuery)
                                    friendQuery = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8), contentColor = Color.Black),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "Adicionar")
                        }
                    }
                }

                items(profile.friends) { f ->
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (f.pendingIncoming) Color(0xFFFACC15) else Color(0xFF1E293B)),
                        modifier = Modifier.clickable { selectedFriendProfile = f }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (f.online) Color(0xFF4ADE80) else Color(0xFF64748B))
                                )
                                Column {
                                    Text("${f.name} (Nv.${f.level})", color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${f.statusText} • 🏆 ${f.trophies}", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (f.pendingIncoming) {
                                    IconButton(onClick = { onAcceptFriend(f.id) }) {
                                        Icon(Icons.Filled.Check, contentDescription = "Aceitar", tint = Color(0xFF4ADE80))
                                    }
                                } else {
                                    Button(
                                        onClick = { onChallengeFriend(f) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFACC15), contentColor = Color.Black),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Filled.SportsKabaddi, contentDescription = "Desafiar", modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Duelo", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                IconButton(onClick = { onRemoveFriend(f.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remover", tint = Color(0xFFF43F5E))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDonationPicker) {
        AlertDialog(
            onDismissRequest = { showDonationPicker = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Pedir Doação de Carta", color = Color.White) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(profile.cardCollection.keys.toList().take(20)) { cid ->
                        val spec = RoyaleCardCatalog.get(cid)
                        if (spec != null) {
                            OutlinedButton(
                                onClick = {
                                    onRequestDonation(cid)
                                    showDonationPicker = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${spec.name} (${spec.rarity.labelPt})", color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDonationPicker = false }) { Text("FECHAR") } }
        )
    }

    if (showCreateClanDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var emblem by remember { mutableStateOf("🛡️") }
        AlertDialog(
            onDismissRequest = { showCreateClanDialog = false },
            containerColor = Color(0xFF0F172A),
            title = { Text("Criar Novo Clã", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do Clã") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Descrição") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("🛡️", "👑", "⚔️", "🔥", "⚡").forEach { e ->
                            Surface(
                                onClick = { emblem = e },
                                color = if (emblem == e) Color(0xFF2563EB) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(e, fontSize = 20.sp, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onCreateClan(name, emblem, desc, 0)
                    showCreateClanDialog = false
                }) { Text("CRIAR") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateClanDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    selectedFriendProfile?.let { friend ->
        AlertDialog(
            onDismissRequest = { selectedFriendProfile = null },
            containerColor = Color(0xFF0F172A),
            title = { Text(friend.name, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nível da Conta: ${friend.level}", color = Color(0xFFCBD5E1))
                    Text("Troféus: 🏆 ${friend.trophies}", color = Color(0xFFFACC15))
                    Text("Status: ${friend.statusText}", color = Color(0xFF94A3B8))
                    Text("Duelos amistosos simulam estratégia IA com regras de nível 11.", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onChallengeFriend(friend)
                    selectedFriendProfile = null
                }) { Text("DESAFIAR (AMISTOSO)") }
            },
            dismissButton = {
                TextButton(onClick = { selectedFriendProfile = null }) { Text("FECHAR") }
            }
        )
    }
}

@Composable
private fun ClanDirectoryView(
    directory: List<ClanDirectoryEntry>,
    onJoinClan: (ClanDirectoryEntry) -> Unit,
    onOpenCreate: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar clã...", color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onOpenCreate, modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Criar")
                }
            }
        }
        items(directory.filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }) { entry ->
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        Text(entry.emblem, fontSize = 24.sp)
                        Column {
                            Text("${entry.name} ${entry.tag}", color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${entry.membersCount}/50 • 🏆 ${entry.totalTrophies} • Mín: ${entry.minTrophies}", color = Color(0xFF94A3B8), fontFamily = JetBrainsMonoFamily, fontSize = 10.sp)
                        }
                    }
                    Button(onClick = { onJoinClan(entry) }) {
                        Text("ENTRAR", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) Color(0xFF2563EB) else Color(0xFF0F172A),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFF60A5FA) else Color(0xFF334155)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = Color.White,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        )
    }
}
