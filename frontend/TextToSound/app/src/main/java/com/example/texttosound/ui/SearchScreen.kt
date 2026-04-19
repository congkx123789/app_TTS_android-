package com.example.texttosound.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.texttosound.viewmodel.BookViewModel

@Composable
fun SearchScreen(
    viewModel: BookViewModel,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        android.util.Log.d("SearchScreen", "SearchScreen Initialized")
    }
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF191A22))
    ) {
        // Top Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 8.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2A2B36))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Placeholder or Text
                    Text(
                        text = if (searchQuery.isEmpty()) "神秘复苏" else searchQuery,
                        color = if (searchQuery.isEmpty()) Color.Gray else Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(24.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
        ) {
            // Search History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("搜索历史", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.Delete, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
                
                // History Tags (Simulated FlowRow with Rows)
                HistoryTagRow(tags = listOf("高武", "凡人修仙传", "凡人不修仙", "玄幻"))
                Spacer(modifier = Modifier.height(8.dp))
                HistoryTagRow(tags = listOf("神魔书", "末世", "苟", "模拟", "仙逆", "完美"))
            }

            // Recommendations Header
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("为你推荐", color = Color(0xFFFE4C2E), fontSize = 16.sp, fontWeight = FontWeight.Bold) // Selected tab color
                    Text("人气标签", color = Color.Gray, fontSize = 14.sp)
                    Text("会员榜", color = Color.Gray, fontSize = 14.sp)
                    Text("月票榜", color = Color.Gray, fontSize = 14.sp)
                    Text("热门榜", color = Color.Gray, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Red indicator line under "为你推荐"
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(Color(0xFFFE4C2E)))
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Rank List + Category List (Side by Side)
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Ranked List
                    Column(modifier = Modifier.weight(1f)) {
                        RankItem(1, "九星神龙诀")
                        RankItem(2, "网游之神级土豪", isHot = true)
                        RankItem(3, "混沌天帝诀")
                        RankItem(4, "九真九阳")
                        RankItem(5, "修真归来在都市")
                        RankItem(6, "九龙葬天经")
                        RankItem(7, "九仙图")
                    }
                    
                    // Spacer between columns
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Simple divider
                    Box(modifier = Modifier.width(1.dp).height(240.dp).background(Color(0xFF2A2B36)))
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    // Categories List
                    Column(modifier = Modifier.width(80.dp)) {
                        CategoryItem("穿越")
                        CategoryItem("热血", color = Color(0xFFF9A825))
                        CategoryItem("轻松")
                        CategoryItem("重生")
                        CategoryItem("至尊王者")
                        CategoryItem("天才流")
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryTagRow(tags: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2A2B36))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(tag, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun RankItem(rank: Int, title: String, isHot: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank text color: 1 is Gold, 2 is Orange, 3 is Light Orange, others gray
        val rankColor = when(rank) {
            1 -> Color(0xFFFFA000)
            2 -> Color(0xFFFF6D00)
            3 -> Color(0xFFFFB300)
            else -> Color.Gray
        }
        Text(
            text = rank.toString(),
            color = rankColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f)
        )
        if (isHot) {
            Text(
                text = "HOT",
                color = Color(0xFFFE4C2E),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun CategoryItem(name: String, color: Color = Color.Gray) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(3.dp).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(name, color = color, fontSize = 14.sp)
    }
}
