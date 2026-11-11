# DreamHouse 3D

DreamHouse 3D là trò chơi xây dựng nhà và cảnh quan 3D. Bản hiện hành **Glade** được viết bằng Java và jMonkeyEngine. Mã nguồn, thư viện cần cho bản dựng và script chạy nằm trong [game-code](game-code/).

Hướng dẫn tính năng và điều khiển: [glade/README.md](glade/README.md).

## Chạy từ mã nguồn

Trên Windows, cài JDK 21 rồi chạy:

```powershell
.\game-code\Start-DreamHouse.ps1 -Rebuild
```

Lệnh này biên dịch mã nguồn và tạo bản portable trong `game-code\release`. Sau đó chạy `CHOI-GAME.cmd` để mở game. Bản portable được tạo ra không nằm trong kho mã nguồn.
