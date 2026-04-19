from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from database import get_db
import models, schemas

router = APIRouter(prefix="/api/v1", tags=["groups"])

@router.get("/groups", response_model=List[schemas.GroupResponse])
def get_groups(db: Session = Depends(get_db)):
    return db.query(models.Group).all()

@router.get("/groups/{group_id}/posts", response_model=List[schemas.GroupPostResponse])
def get_group_posts(group_id: int, db: Session = Depends(get_db)):
    group = db.query(models.Group).filter(models.Group.id == group_id).first()
    if not group:
        raise HTTPException(status_code=404, detail="Group not found")
    return db.query(models.GroupPost).filter(models.GroupPost.group_id == group_id).all()
