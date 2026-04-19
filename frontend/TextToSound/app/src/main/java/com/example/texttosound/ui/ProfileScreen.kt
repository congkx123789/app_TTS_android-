package com.example.texttosound.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.texttosound.viewmodel.BookViewModel

@Composable
fun ProfileScreen(viewModel: BookViewModel) {
    val scrollState = rememberScrollState()
    val userProfile by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191A22))
            .verticalScroll(scrollState)
            .padding(bottom = 100.dp) // Space for bottom nav and player
    ) {
        // User Profile Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                if (userProfile?.avatarUrl != null) {
                    AsyncImage(
                        model = userProfile!!.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Person, contentDescription = "Avatar", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                }
            }
            
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = userProfile?.name ?: "Đang tải...",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tham gia ${userProfile?.daysOnPlatform ?: 0} ngày · Nhận ${userProfile?.badgeCount ?: 0} huy hiệu",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = userProfile?.bio ?: "Chưa có giới thiệu",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        // VIP Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF333333))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hội viên Trả phí", color = Color(0xFFFFD700), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(" Đã tiết kiệm 8453đ >", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Gia hạn", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("2026-03-18 đến hạn", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Account / Wallet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2A2B36))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tài khoản của tôi", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AccountStatItem("0", "Xu đọc")
                AccountStatItem("1040", "Xu tặng")
                AccountStatItem("1", "Vé tháng")
                
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Nạp tiền", color = Color(0xFFFF4081), fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider(color = Color(0xFF333333))
            Spacer(modifier = Modifier.height(24.dp))
            
            // Grid of Actions
            val iconTint = Color.LightGray
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileGridItem(Icons.Filled.Notifications, "Tin nhắn", iconTint)
                    ProfileGridItem(Icons.Filled.MilitaryTech, "Cấp độ", iconTint)
                    ProfileGridItem(Icons.Filled.FavoriteBorder, "Theo dõi", iconTint)
                    ProfileGridItem(Icons.Filled.Edit, "Đã đăng", iconTint)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ProfileGridItem(Icons.Filled.Checkroom, "Trang phục", iconTint)
                    ProfileGridItem(Icons.Filled.Style, "Thẻ bài", iconTint)
                    ProfileGridItem(Icons.Filled.WorkspacePremium, "Huy chương", iconTint)
                    ProfileGridItem(Icons.Filled.Biotech, "Gen", iconTint)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Box(modifier = Modifier.weight(1f)) { ProfileGridItem(Icons.Filled.AutoStories, "Lịch sử đọc", iconTint) }
                    Box(modifier = Modifier.weight(1f)) { ProfileGridItem(Icons.Filled.History, "Lịch sử duyệt", iconTint) }
                    Spacer(modifier = Modifier.weight(2f)) // Fill remaining space for alignment
                }
            }
        }
    }
}

@Composable
fun AccountStatItem(value: String, label: String) {
    Column {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun ProfileGridItem(icon: ImageVector, label: String, tint: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, color = Color.LightGray, fontSize = 12.sp)
    }
}
