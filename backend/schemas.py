from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

class UserBase(BaseModel):
    name: str
    avatar_url: Optional[str] = None
    bio: Optional[str] = None
    days_on_platform: int = 0
    badge_count: int = 0

class UserResponse(UserBase):
    id: int
    class Config:
        from_attributes = True

class BookBase(BaseModel):
    title: str
    author: str
    cover_url: Optional[str] = None
    genre: str
    rating: float = 0.0
    chapter_count: int = 0
    description: Optional[str] = None
    reader_count_info: Optional[str] = None

class BookResponse(BookBase):
    id: int
    class Config:
        from_attributes = True

class LibraryBookResponse(BookResponse):
    progress_percent: float
    last_read_at: datetime
    class Config:
        from_attributes = True

class PostBase(BaseModel):
    content: str
    likes_count: int = 0
    comments_count: int = 0

class PostResponse(PostBase):
    id: int
    author_id: int
    author: UserResponse
    created_at: datetime
    class Config:
        from_attributes = True

class GroupBase(BaseModel):
    name: str
    cover_url: Optional[str] = None
    member_count: Optional[str] = None
    activity_info: Optional[str] = None

class GroupResponse(GroupBase):
    id: int
    class Config:
        from_attributes = True

class GroupPostResponse(PostBase):
    id: int
    group_id: int
    author_id: int
    created_at: datetime
    class Config:
        from_attributes = True
