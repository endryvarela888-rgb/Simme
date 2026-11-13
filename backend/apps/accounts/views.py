from rest_framework import generics, permissions
from rest_framework.response import Response
from rest_framework_simplejwt.views import TokenObtainPairView
from rest_framework_simplejwt.views import TokenBlacklistView
from .models import User
from .serializers import RegisterSerializer

class RegisterView(generics.CreateAPIView):
    queryset = User.objects.all()
    serializer_class = RegisterSerializer
    permission_classes = [permissions.AllowAny]

class EmailTokenView(TokenObtainPairView):
    pass

class MeView(generics.GenericAPIView):
    permission_classes = [permissions.IsAuthenticated]
    def get(self, request):
        return Response({"id": request.user.id, "email": request.user.email, "display_name": request.user.display_name})

class LogoutView(TokenBlacklistView):
    permission_classes = [permissions.IsAuthenticated]
