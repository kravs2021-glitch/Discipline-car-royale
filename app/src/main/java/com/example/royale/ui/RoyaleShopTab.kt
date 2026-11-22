package com.example.royale.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.royale.RoyaleCardCatalog
import com.example.royale.data.RoyaleProfileState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SpaceGroteskFamily

@Composable
fun RoyaleShopTab(
    profile: RoyaleProfileState,
    catalog: List<ShopOfferItem>,
    onBuyOffer: (ShopOfferItem) -> Unit
) {
    var pendingConfirmOffer by remember { mutableStateOf<ShopOfferItem?>(null) }
    val grouped = remember(catalog) { catalog.groupBy { it.section } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
                    Column {
                        Text("LOJA REAL", color = Color.White, fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text(
                            text = "Ofertas permanentes, cartas, itens mágicos e cosméticos com confirmação.",
                            color = Color(0xFF94A3B8),
                            fontFamily = SpaceGroteskFamily,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        grouped.forEach { (section, items) ->
            item {
                Text(
                    text = section.uppercase(),
                    color = Color(0xFFFACC15),
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }

            val chunked = items.chunked(2)
            items(chunked.size) { rowIdx ->
                val pair = chunked[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { offer ->
                        val alreadyClaimed = offer.id in profile.shopBoughtTodayIds ||
                            (offer.emoteId != null && offer.emoteId in profile.unlockedEmotes) ||
                            (offer.skinName != null && offer.skinName in profile.unlockedSkins) ||
                            (offer.towerTroop != null && offer.towerTroop in profile.unlockedTowerTroops)
                        ShopOfferCard(
                            offer = offer,
                            alreadyOwned = alreadyClaimed,
                            onClick = { if (!alreadyClaimed) pendingConfirmOffer = offer },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    pendingConfirmOffer?.let { offer ->
        val costText = when {
            offer.costGold == 0 && offer.costGems == 0 -> "GRÁTIS"
            offer.costGold > 0 -> "🪙 ${offer.costGold} Ouro"
            else -> "💎 ${offer.costGems} Gemas"
        }
        AlertDialog(
            onDismissRequest = { pendingConfirmOffer = null },
            containerColor = Color(0xFF0F172A),
            title = { Text("Confirmar Compra", color = Color.White, fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Deseja adquirir '${offer.title}' por $costText?",
                    color = Color(0xFFCBD5E1),
                    fontFamily = SpaceGroteskFamily
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onBuyOffer(offer)
                        pendingConfirmOffer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
                ) {
                    Text("CONFIRMAR", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmOffer = null }) {
                    Text("CANCELAR", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun ShopOfferCard(
    offer: ShopOfferItem,
    alreadyOwned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardSpec = offer.cardId?.let { RoyaleCardCatalog.get(it) }
    val priceLabel = when {
        alreadyOwned -> "ADQUIRIDO"
        offer.costGold == 0 && offer.costGems == 0 -> "RESGATAR GRÁTIS"
        offer.costGold > 0 -> "🪙 ${offer.costGold}"
        else -> "💎 ${offer.costGems}"
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(176.dp).testTag("shop_offer_${offer.id}"),
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, if (alreadyOwned) Color(0xFF334155) else Color(offer.accentHex))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = offer.title,
                color = Color.White,
                fontFamily = SpaceGroteskFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 2
            )

            if (cardSpec != null) {
                CardIllustration(spec = cardSpec, modifier = Modifier.size(54.dp))
            } else if (offer.chestType != null) {
                ChestIllustration(type = offer.chestType, modifier = Modifier.size(54.dp))
            } else {
                Text(
                    text = when {
                        offer.grantGold > 0 -> "🪙"
                        offer.magicItem != null -> "✨"
                        offer.emoteId != null -> "😄"
                        offer.towerTroop != null -> "🏰"
                        else -> "🎨"
                    },
                    fontSize = 30.sp
                )
            }

            Text(
                text = offer.subtitle,
                color = Color(0xFF94A3B8),
                fontFamily = SpaceGroteskFamily,
                fontSize = 10.sp,
                maxLines = 2
            )

            Surface(
                color = if (alreadyOwned) Color(0xFF1E293B) else Color(offer.accentHex),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (alreadyOwned) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(14.dp))
                    }
                    Text(
                        text = priceLabel,
                        color = if (alreadyOwned) Color(0xFF4ADE80) else Color(0xFF09090B),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
