# DreamHouse 3D

DreamHouse 3D là trò chơi xây dựng nhà và cảnh quan 3D. Bản hiện hành **Glade** được viết bằng Java và jMonkeyEngine. Mã nguồn, thư viện cần cho bản dựng và script chạy nằm trong [game-code](game-code/).

Hướng dẫn tính năng và điều khiển: [glade/README.md](glade/README.md). Hướng dẫn bắt đầu chơi: [BAT-DAU-CHOI.md](BAT-DAU-CHOI.md). Thông tin cấu trúc chương trình: [THONG-TIN-DREAMHOUSE3D.json](THONG-TIN-DREAMHOUSE3D.json).

## Chạy từ mã nguồn

Trên Windows, cài JDK 21 rồi mở PowerShell tại thư mục dự án và chạy:

```powershell
.\game-code\Start-DreamHouse.ps1 -Rebuild
```

Lệnh này biên dịch mã nguồn và tạo bản portable trong `game-code\release`. Sau khi tạo xong, chạy `CHOI-GAME.cmd`. Bản portable được tạo ra không nằm trong kho mã nguồn.

Để chạy kiểm tra dữ liệu và bộ đệm mô hình, dùng `.\KIEM-TRA.cmd`. Muốn chạy thêm các kiểm tra đồ họa, dùng `.\KIEM-TRA.cmd -IncludeGraphics`.

## Bản Classic

Bản JavaFX trước đây nằm trong `game-code\classic`. Cần JDK 21; chạy `Mo-DreamHouse-Classic.cmd` hoặc vào thư mục đó và dùng Maven với lệnh `mvn javafx:run`.
