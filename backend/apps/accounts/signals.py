from django.db.models.signals import post_delete
from django.dispatch import receiver

from .models import User


@receiver(post_delete, sender=User)
def delete_user_avatar_file(sender, instance, **kwargs):
    if instance.avatar:
        instance.avatar.delete(save=False)
