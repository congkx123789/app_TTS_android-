from sqlalchemy.orm import Session
from database import SessionLocal, engine, Base
import models
from datetime import datetime

# Create tables
Base.metadata.create_all(bind=engine)

def seed_db():
    db = SessionLocal()
    try:
        # Check if already seeded
        if db.query(models.User).first():
            print("Database already has data. Skipping seed.")
            return

        # 1. Create Users
        user1 = models.User(
            name="Nhất Khúc Tương Tư",
            avatar_url="https://i.pravatar.cc/150?u=user1",
            bio="Viết về thế giới theo cách của mình.",
            days_on_platform=483,
            badge_count=38
        )
        user2 = models.User(
            name="Một Cốc Băng Lạc",
            avatar_url="https://i.pravatar.cc/150?u=user2",
            bio="Đam mê đọc sách và chia sẻ.",
            days_on_platform=120,
            badge_count=12
        )
        user3 = models.User(
            name="Kim Chi",
            avatar_url="https://i.pravatar.cc/150?u=user3",
            bio="Cuộc sống là những chuyến đi.",
            days_on_platform=30,
            badge_count=5
        )
        db.add_all([user1, user2, user3])
        db.commit()

        # 2. Create Books
        book1 = models.Book(
            title="Vũ Luyện Điên Phong",
            author="Mạc Mặc",
            cover_url="https://picsum.photos/id/1/200/300",
            genre="Huyền huyễn",
            rating=4.8,
            chapter_count=6000,
            description="Đỉnh phong của võ đạo, là cô độc, là tịch mịch...",
            reader_count_info="6.1 vạn người đang đọc"
        )
        book2 = models.Book(
            title="Tiên Nghịch",
            author="Nhĩ Căn",
            cover_url="https://picsum.photos/id/2/200/300",
            genre="Tiên hiệp",
            rating=4.9,
            chapter_count=2000,
            description="Một người bình thường bước trên con đường tu tiên đầy gian khổ...",
            reader_count_info="4.5 vạn người đang đọc"
        )
        book3 = models.Book(
            title="Đấu Phá Thương Khung",
            author="Thiên Tằm Thổ Đậu",
            cover_url="https://picsum.photos/id/3/200/300",
            genre="Huyền huyễn",
            rating=4.7,
            chapter_count=1600,
            description="Ba mươi năm Hà Đông, ba mươi năm Hà Tây, đừng khinh thiếu niên nghèo!",
            reader_count_info="8.2 vạn người đang đọc"
        )
        db.add_all([book1, book2, book3])
        db.commit()

        # 3. Add Books to Library (User1)
        user1.library_books.extend([book1, book2])
        db.commit()

        # 4. Create Posts (Explore)
        post1 = models.Post(
            author_id=user1.id,
            content="Bính Ngọ năm Tây Du nhàn đàm lục. Hôm nay tôi có ý tưởng mới về việc viết nhật ký du lịch Tây Du Ký.",
            likes_count=46,
            comments_count=5
        )
        post2 = models.Post(
            author_id=user2.id,
            content="Mọi người đã đọc chương mới nhất của Vũ Luyện Điên Phong chưa? Thật sự quá kịch tính!",
            likes_count=120,
            comments_count=35
        )
        db.add_all([post1, post2])
        db.commit()

        # 5. Create Groups
        group1 = models.Group(
            name="CHỢ ÔTÔ - XE MÁY CŨ CẨM PHẢ",
            cover_url="https://picsum.photos/id/10/800/400",
            member_count="120K members",
            activity_info="20+ posts a day"
        )
        group2 = models.Group(
            name="Hội Hyundai Sonata Việt Nam™",
            cover_url="https://picsum.photos/id/11/800/400",
            member_count="45K members",
            activity_info="5+ posts a day"
        )
        db.add_all([group1, group2])
        db.commit()

        # 6. Create Group Posts
        gp1 = models.GroupPost(
            group_id=group1.id,
            author_id=user3.id,
            content="🚗 Cần bán xe Honda Civic RS sản xuất 2020, xe cực đẹp, bao check hãng.",
            likes_count=11,
            comments_count=4
        )
        db.add_all([gp1])
        db.commit()

        print("Database seeded successfully!")
    except Exception as e:
        print(f"Error seeding database: {e}")
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    seed_db()
