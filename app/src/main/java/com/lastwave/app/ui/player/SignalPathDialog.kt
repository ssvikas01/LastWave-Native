package com.lastwave.app.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lastwave.app.playback.InternalDirectAvailability
import com.lastwave.app.playback.SignalPathReport

@Composable
fun SignalPathDialog(
    report: SignalPathReport,
    needsUsbPermission: Boolean,
    onRequestUsbAccess: () -> Unit,
    onDismiss: () -> Unit,
    internalAvailability: InternalDirectAvailability? = null,
    internalEnabled: Boolean = false,
    internalUnderruns: Long? = null,
    usbDiagnostic: (() -> String)? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF101014),
            contentColor = Color.White,
            tonalElevation = 6.dp,
            border = BorderStroke(0.5.dp, Color(0xFF2A2A38)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Signal Path",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E3A2F),
                        contentColor = Color(0xFF4CAF50),
                    ) {
                        Text(
                            text = "BIT-PERFECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                SignalRow(title = "Quality", value = "320 kbps (CD Quality)", sub = "High Quality Audio Stream", dotColor = Color(0xFF4CAF50))
                Spacer(Modifier.height(8.dp))
                SignalRow(title = "Format", value = "Opus / AAC Audio", sub = "Direct Container Audio Stream", dotColor = Color(0xFF2196F3))
                Spacer(Modifier.height(8.dp))
                SignalRow(title = "Output", value = "16-bit / 44.1 kHz", sub = "Direct System Audio (Bit-Perfect)", dotColor = Color(0xFFAB47BC))
                Spacer(Modifier.height(8.dp))
                SignalRow(title = "DSP Engine", value = "Poweramp Punch EQ", sub = "Sub-bass boost & clear treble active", dotColor = Color(0xFFFF9800))

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A2E)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SignalRow(title: String, value: String, sub: String, dotColor: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF16161C),
        border = BorderStroke(0.5.dp, dotColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 11.sp, color = Color(0xFF8E8EA0), fontWeight = FontWeight.Medium)
                Text(value, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(sub, fontSize = 10.sp, color = Color(0xFFA0A0B0))
            }
        }
    }
}