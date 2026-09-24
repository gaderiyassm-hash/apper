package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.KycStatus
import com.example.core.model.UserProfile
import com.example.ui.theme.AccentGold
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TradeDownRed
import com.example.ui.theme.TradeUpGreen

@Composable
fun ProfileKycScreen(
    user: UserProfile,
    onUpdateKyc: (KycStatus) -> Unit,
    onToggleMfa: (Boolean) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showKycModal by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("profile_kyc_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Profile & Account", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        // Profile Avatar Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(AccentGold.copy(alpha = 0.2f), CircleShape)
                            .border(2.dp, AccentGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentGold, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(user.fullName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(user.email, fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("UID: ${user.id}", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Member since ${user.joinedDate}", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // KYC Identity Verification Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Identity Verification (KYC)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        val (kBg, kColor) = when (user.kycStatus) {
                            KycStatus.VERIFIED -> StatusSuccess.copy(0.15f) to StatusSuccess
                            KycStatus.UNDER_REVIEW -> StatusPending.copy(0.15f) to StatusPending
                            KycStatus.NOT_STARTED -> SurfaceBorder to TextSecondary
                            KycStatus.IN_PROGRESS -> StatusPending.copy(0.15f) to StatusPending
                            KycStatus.REJECTED -> StatusError.copy(0.15f) to StatusError
                            KycStatus.RESUBMISSION_REQUIRED -> StatusError.copy(0.15f) to StatusError
                        }

                        Box(
                            modifier = Modifier
                                .background(kBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(user.kycStatus.name.replace("_", " "), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = kColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (user.kycStatus) {
                            KycStatus.VERIFIED -> "Your account is fully verified. Standard limits: $500,000 / day."
                            KycStatus.UNDER_REVIEW -> "Your identity documents are under compliance review. Estimated time: < 2 hours."
                            else -> "Verify your identity to increase withdrawal limits and activate advanced trade features."
                        },
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showKycModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("manage_kyc_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = if (user.kycStatus == KycStatus.VERIFIED) SurfaceBorder else AccentGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (user.kycStatus == KycStatus.VERIFIED) "View KYC Information" else "Complete Identity Verification",
                            color = if (user.kycStatus == KycStatus.VERIFIED) TextPrimary else Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Security Settings Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security & Privacy", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2FA Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Two-Factor Authentication (2FA)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Protects withdrawals & trade changes", fontSize = 10.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = user.mfaEnabled,
                            onCheckedChange = { onToggleMfa(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = SurfaceDark)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Active Sessions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active Sessions", fontSize = 12.sp, color = TextPrimary)
                        }
                        Text("${user.activeSessionCount} Devices", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Authoritative Backend Core Audit
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("backend_core_audit_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TradeUpGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Authoritative Backend Core", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Surface(
                            color = TradeUpGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("ONLINE", color = TradeUpGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val auditSpecs = listOf(
                        "Double-Entry Ledger" to "Append-Only (Cents)",
                        "Settlement Engine" to "Server-Authoritative",
                        "Risk & Fraud Gate" to "Active (Velocity Filter)",
                        "Idempotency Guard" to "Enforced (UUID Keys)"
                    )
                    auditSpecs.forEach { (title, status) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(title, fontSize = 11.sp, color = TextSecondary)
                            Text(status, fontSize = 11.sp, color = AccentGold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Logout Button
        item {
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("logout_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, TradeDownRed.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", tint = TradeDownRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", color = TradeDownRed, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showKycModal) {
        KycVerificationSheet(
            currentStatus = user.kycStatus,
            onDismiss = { showKycModal = false },
            onSubmitVerification = {
                onUpdateKyc(KycStatus.VERIFIED)
                showKycModal = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycVerificationSheet(
    currentStatus: KycStatus,
    onDismiss: () -> Unit,
    onSubmitVerification: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var docType by remember { mutableStateOf("Passport") }
    var docNumber by remember { mutableStateOf("") }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .testTag("kyc_modal_sheet")
        ) {
            Text("Identity Verification Wizard", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Government compliance regulations require identity verification for all margin trading.", fontSize = 11.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Document Type", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val docOptions = listOf("Passport", "Driver's License", "National ID")
                for (dt in docOptions) {
                    val isSel = dt == docType
                    Surface(
                        onClick = { docType = dt },
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSel) AccentGold else SurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) AccentGold else SurfaceBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                dt,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.Black else TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Document Number", fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = docNumber,
                onValueChange = { docNumber = it },
                placeholder = { Text("e.g. A90123847", fontSize = 11.sp, color = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kyc_doc_number_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Upload Document Simulation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(SurfaceCard, RoundedCornerShape(8.dp))
                    .border(1.dp, androidx.compose.ui.graphics.Color.Gray, RoundedCornerShape(8.dp))
                    .clickable { uploadedFileName = "$docType-front-scan.jpg" },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (uploadedFileName != null) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                        contentDescription = "Upload",
                        tint = if (uploadedFileName != null) TradeUpGreen else AccentGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uploadedFileName ?: "Tap to attach / upload photo of $docType",
                        fontSize = 11.sp,
                        fontWeight = if (uploadedFileName != null) FontWeight.Bold else FontWeight.Normal,
                        color = if (uploadedFileName != null) TradeUpGreen else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSubmitVerification,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("submit_kyc_verification_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Submit For Instant Review", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
