from django.contrib.auth.password_validation import validate_password
from rest_framework import serializers
from .models import User

class RegisterSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, validators=[validate_password])
    class Meta:
        model = User
        fields = ("email", "password", "display_name")
    def create(self, validated_data):
        return User.objects.create_user(**validated_data)

class EmailAddressSerializer(serializers.Serializer):
    email = serializers.EmailField()
    def validate_email(self, value): return value.strip().lower()

class EmailCodeSerializer(EmailAddressSerializer):
    code = serializers.RegexField(r"^\d{6}$")

class PasswordResetSerializer(EmailCodeSerializer):
    new_password = serializers.CharField(write_only=True, validators=[validate_password])
