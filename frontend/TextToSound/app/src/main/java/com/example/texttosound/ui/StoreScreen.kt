package com.example.texttosound.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.texttosound.viewmodel.BookViewModel

@Composable
fun StoreScreen(
    viewModel: BookViewModel,
    onNavigateToSearch: () -> Unit
) {
    val storeBooks by viewModel.storeBooks.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191A22))
    ) {
        // Top Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Nam", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(32.dp))
            Text("Nữ", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        }

        // Search Bar Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2A2B36))
                .clickable { 
                    android.util.Log.d("StoreScreen", "Search bar clicked in StoreScreen")
                    onNavigateToSearch() 
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Võ Luyện Điên Phong", color = Color.Gray, fontSize = 14.sp)
            }
        }

        // Horizontal Scroll Sections
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                StoreHorizontalSection(title = "Sách mới cập nhật", books = storeBooks)
            }
            item {
                StoreHorizontalSection(title = "Đang thịnh hành", books = storeBooks.shuffled())
            }
            
            // Promo Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2B36))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).background(Color.Red))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Quyền lợi VIP", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF444444)))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Tặng bạn thân thẻ VIP 3 ngày, khám phá niềm vui bất ngờ", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }

            item {
                StoreHorizontalSection(title = "Gợi ý cho bạn", books = storeBooks.reversed())
            }
        }
    }
}

@Composable
fun StoreHorizontalSection(title: String, books: List<com.example.texttosound.api.Book>) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Xem thêm >", color = Color.Gray, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            books.forEach { book ->
                StoreBookItem(book)
            }
        }
    }
}

@Composable
fun StoreBookItem(book: com.example.texttosound.api.Book) {
    Column(modifier = Modifier.width(100.dp)) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        ) {
            if (book.coverUrl != null) {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = book.author,
            color = Color.Gray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
