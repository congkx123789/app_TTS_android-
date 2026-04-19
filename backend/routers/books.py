from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from database import get_db
import models, schemas

router = APIRouter(prefix="/api/v1", tags=["books"])

@router.get("/books", response_model=List[schemas.BookResponse])
def get_books(db: Session = Depends(get_db)):
    return db.query(models.Book).all()

@router.get("/books/{book_id}", response_model=schemas.BookResponse)
def get_book(book_id: int, db: Session = Depends(get_db)):
    book = db.query(models.Book).filter(models.Book.id == book_id).first()
    if not book:
        raise HTTPException(status_code=404, detail="Book not found")
    return book

@router.get("/users/{user_id}/library", response_model=List[schemas.LibraryBookResponse])
def get_user_library(user_id: int, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    # In a real app we'd join user_books to get progress_percent
    # For now we'll just return the books with dummy progress
    library = []
    for book in user.library_books:
        # Simulate progress data which is normally in the association table
        lib_book = schemas.LibraryBookResponse(
            **book.__dict__,
            progress_percent=45.0, # Dummy
            last_read_at=book.__dict__.get('last_read_at') or "2026-02-28T12:00:00"
        )
        library.append(lib_book)
    return library
