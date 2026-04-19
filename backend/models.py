from sqlalchemy import Column, Integer, String, Float, Text, DateTime, ForeignKey, Table
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database import Base

# Association table for User-Book (Library)
user_book_association = Table(
    'user_books',
    Base.metadata,
    Column('user_id', Integer, ForeignKey('users.id')),
    Column('book_id', Integer, ForeignKey('books.id')),
    Column('progress_percent', Float, default=0.0),
    Column('last_read_at', DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
)

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    avatar_url = Column(String, nullable=True)
    bio = Column(Text, nullable=True)
    days_on_platform = Column(Integer, default=0)
    badge_count = Column(Integer, default=0)
    
    posts = relationship("Post", back_populates="author")
    library_books = relationship("Book", secondary=user_book_association, back_populates="readers")

class Book(Base):
    __tablename__ = "books"

    id = Column(Integer, primary_key=True, index=True)
    title = Column(String, index=True)
    author = Column(String, index=True)
    cover_url = Column(String, nullable=True)
    genre = Column(String, index=True)
    rating = Column(Float, default=0.0)
    chapter_count = Column(Integer, default=0)
    description = Column(Text, nullable=True)
    reader_count_info = Column(String, nullable=True) # e.g. "6.1 vạn người đang đọc"

    readers = relationship("User", secondary=user_book_association, back_populates="library_books")

class Post(Base):
    __tablename__ = "posts"

    id = Column(Integer, primary_key=True, index=True)
    author_id = Column(Integer, ForeignKey("users.id"))
    content = Column(Text)
    likes_count = Column(Integer, default=0)
    comments_count = Column(Integer, default=0)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    author = relationship("User", back_populates="posts")

class Group(Base):
    __tablename__ = "groups"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    cover_url = Column(String, nullable=True)
    member_count = Column(String, nullable=True) # e.g. "1.2M members"
    activity_info = Column(String, nullable=True) # e.g. "10+ posts a day"

    posts = relationship("GroupPost", back_populates="group")

class GroupPost(Base):
    __tablename__ = "group_posts"

    id = Column(Integer, primary_key=True, index=True)
    group_id = Column(Integer, ForeignKey("groups.id"))
    author_id = Column(Integer, ForeignKey("users.id"))
    content = Column(Text)
    likes_count = Column(Integer, default=0)
    comments_count = Column(Integer, default=0)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    group = relationship("Group", back_populates="posts")
