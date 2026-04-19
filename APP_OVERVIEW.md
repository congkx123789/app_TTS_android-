# 📱 Trudio - The Ultimate Text-to-Sound Experience

Trudio là một ứng dụng di động hiện đại được thiết kế để chuyển đổi trải nghiệm đọc sách thành nghe sách một cách mượt mà nhất. Với tập trung đặc biệt vào ngôn ngữ Tiếng Việt, Trudio kết hợp sức mạnh của AI với giao diện người dùng cao cấp để tạo ra một hệ sinh thái sách nói thông minh.

---

## 🚀 Tính năng cốt lõi (Core Features)

### 1. Trình đọc EPUB thông minh (EPUB Reader)
*   **Đọc và Nghe đồng thời**: Tích hợp trình đọc EPUB cho phép người dùng vừa theo dõi văn bản vừa nghe giọng đọc AI.
*   **Đồng bộ hóa**: Tiến trình đọc được lưu trữ và đồng bộ hóa giữa các thiết bị.

### 2. Công nghệ AI Text-to-Speech (TTS) Đa dạng
Trudio không chỉ sử dụng một giọng đọc duy nhất mà cung cấp một danh sách các "nghệ sĩ AI" hàng đầu:
*   **Matcha-TTS (VN)**: Giọng đọc Tiếng Việt tự nhiên, tốc độ cao và cảm xúc.
*   **Piper (ZaloPay)**: Giọng đọc mượt mà, chuyên nghiệp từ các engine hàng đầu.
*   **VieNeu (GGUF/LLM)**: Sử dụng các mô hình ngôn ngữ lớn để tạo ra giọng đọc có hiểu biết sâu sắc về ngữ cảnh.
*   **F5-TTS**: Công nghệ khuếch tán (Diffusion) cho chất lượng âm thanh trung thực nhất.

### 3. Trình phát Mini Nổi (Floating Mini Player)
*   **Đa nhiệm (Multitasking)**: Cho phép người dùng nghe sách trong khi đang lướt thư viện hoặc tham gia cộng đồng.
*   **Điều khiển nhanh**: Play, Pause, đóng sách ngay từ pill overlay siêu nhỏ gọn.

### 4. Hệ sinh thái Cộng đồng (Social Hub)
*   **Nhóm (Groups)**: Tham gia các cộng đồng yêu sách, trao đổi kinh nghiệm.
*   **Bài viết (Posts)**: Chia sẻ cảm nhận, đánh giá về các tác phẩm.
*   **Thư viện cá nhân**: Quản lý kệ sách, theo dõi tiến trình đọc và đánh giá.

---

## 🛠️ Kiến trúc Kỹ thuật (Technical Stack)

### Frontend (Android Native)
*   **Ngôn ngữ**: Kotlin
*   **UI Framework**: Jetpack Compose (Modern, Declarative UI)
*   **Data Handling**: ViewModel + Coroutines Flow
*   **Storage**: Room (Local Cache/Library) + DataStore
*   **Networking**: Retrofit + OkHttp

### Backend (Distributed API)
*   **Framework**: FastAPI (Python) - Hiệu năng cao, async-first.
*   **Database**: SQLite + SQLAlchemy (Quản lý sách, người dùng, bài viết).
*   **AI Engine**: PyTorch, Llama.cpp, OnnxRuntime.
*   **Storage**: Hệ thống quản lý cache âm thanh hiệu quả.

---

## 🏗️ Cấu trúc Module

### 1. `backend/`
Nơi chứa toàn bộ logic xử lý dữ liệu và điều phối các mô hình TTS.
*   `routers/`: Quản lý các endpoint cho Books, Posts, Users.
*   `piper_handler.py`: Interface kết nối với các mô hình Piper/VieNeu.
*   `main.py`: Entry point của server, quản lý vòng đời ứng dụng.

### 2. `frontend/TextToSound/`
Mã nguồn ứng dụng Android.
*   `ui/`: Các màn hình chính như `LibraryScreen`, `AudiobookPlayerScreen`.
*   `viewmodel/`: Xử lý logic nghiệp vụ và trạng thái dữ liệu.
*   `api/`: Định nghĩa các interface giao tiếp với Backend.

---

## 🎯 Tại sao chọn Trudio?
Trudio không chỉ là một ứng dụng đọc sách. Đó là một công cụ giúp mọi người tiếp cận tri thức nhanh hơn, tiện lợi hơn. Đặc biệt với các mô hình AI tối ưu cho Tiếng Việt, Trudio mang đến cảm giác như đang nghe một người bạn đọc sách cho mình nghe.

---

> [!TIP]
> **Hướng dẫn khởi chạy nhanh:**
> 1. Chạy Backend: `cd backend && bash start_server.sh`
> 2. Chạy Emulator: `~/Android/Sdk/emulator/emulator -avd Pixel_8`
> 3. Cài đặt và mở app từ Android Studio.
