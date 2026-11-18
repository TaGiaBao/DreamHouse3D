# DreamHouse 3D

Game xây dựng nhà và thiết kế cảnh quan 3D trên Windows. Bạn có thể ghép công trình, chỉnh địa hình, vẽ dòng nước và đi bộ qua khu phố do mình dựng. Đây là bài tập lớn cá nhân môn **Kỹ năng lập trình nâng cao** tại Trường Đại học CMC.

**[Tải bản Windows](https://github.com/TaGiaBao/DreamHouse3D/releases/tag/glade-windows-20261114-walk)** · [Hướng dẫn chơi](BAT-DAU-CHOI.md) · [Báo cáo đồ án](reports/Bao_cao_Bai_tap_lon_DreamHouse_BIT247887.docx)

## Chạy game

1. Trong mục **Assets** của bản phát hành, tải `DreamHouse-Glade-Windows.zip`.
2. Giải nén toàn bộ ZIP và mở `DreamHouse.exe` trong thư mục vừa giải nén. Gói đã kèm Java.
3. Bấm **Chơi ngay**. Trong game, mở menu cảnh để thử **Trùng Khánh** hoặc **Vieille Ville**.

## Tính năng

- Dựng, xoay, đổi kích thước và vật liệu của các khối nhà. Ghép mái với tháp hoặc chồng tầng để tạo cụm công trình.
- Đắp hoặc hạ địa hình, vẽ đường và dòng nước, tạo thác từ vùng đất cao.
- Đi bộ qua cầu và cầu thang để quan sát công trình ở tầm mắt.
- Lưu và mở lại cả thung lũng, hoặc lưu một cụm công trình để đặt lại ở cảnh khác.

## Kiến trúc chương trình

Java 21 và jMonkeyEngine 3.9 xử lý game 3D. Swing được dùng cho launcher và các hộp thoại tệp.

| Thành phần | Vai trò |
| --- | --- |
| [`GladeDesign`](game-code/glade/src/main/java/vn/dreamhouse/glade/GladeDesign.java) | Mô hình cảnh, công trình, địa hình và liên kết cấu kiện |
| [`ArchitectureJoin`](game-code/glade/src/main/java/vn/dreamhouse/glade/ArchitectureJoin.java) | Xử lý phần hình học giao nhau khi ghép công trình |
| [`WalkController`](game-code/glade/src/main/java/vn/dreamhouse/glade/WalkController.java) | Di chuyển, độ cao bề mặt và va chạm trong chế độ đi bộ |
| [`WorldFiles`](game-code/glade/src/main/java/vn/dreamhouse/glade/WorldFiles.java) | Ghi, đọc và kiểm tra tính toàn vẹn của bản lưu |

Xem thêm [mã nguồn và quy chuẩn vật liệu](game-code/README.md) và [kế hoạch phát triển](plans/KE-HOACH-DREAMHOUSE-1-NAM.json).

## Biên dịch và kiểm thử

Trên Windows có JDK 21, chạy từ thư mục gốc dự án.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\game-code\Build-DreamHouse.ps1
.\CHOI-GAME.cmd
.\KIEM-TRA.cmd
```

Chạy `.\KIEM-TRA.cmd -IncludeGraphics` để kiểm tra thêm các thao tác trong cửa sổ game. [GitHub Actions](.github/workflows/dreamhouse-checks.yml) chạy nhóm kiểm thử dữ liệu, phục hồi, nhập tệp và bộ nhớ đệm trên Windows.
