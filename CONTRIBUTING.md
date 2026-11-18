# Đóng góp và kiểm tra thay đổi

DreamHouse hiện hỗ trợ Windows x64, JDK 21 trở lên. Chưa xác nhận Linux hoặc macOS.

## Bắt đầu từ mã nguồn sạch

1. Clone repo và checkout nhánh/commit cần kiểm tra; không sao chép thư mục build từ máy khác.
2. Cài JDK 21 có `javac` và `jpackage`; đặt `JAVA_HOME` tới thư mục JDK.
3. Chạy `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\game-code\Build-DreamHouse.ps1` từ gốc repo.
4. Mở `CHOI-GAME.cmd`. Không cần Maven để dựng bản Glade này; các thư viện nằm trong `game-code/glade/lib`.
5. Chạy `KIEM-TRA.cmd`; chạy thêm `KIEM-TRA.cmd -IncludeGraphics` trên desktop Windows có OpenGL khi sửa thao tác hoặc dựng hình.

## Phạm vi pull request

- Mỗi PR xử lý một nhóm hành vi; giải thích lỗi, cách tái hiện và kết quả sau sửa.
- Giữ logic dữ liệu độc lập với cửa sổ game khi có thể. Đưa các phép biến đổi cảnh vào lớp riêng và kiểm thử không cần renderer.
- Khi sửa ghép khối, kiểm tra cả di chuyển, đổi kích thước, sao chép, undo/redo và lưu/mở lại.
- Không commit dữ liệu người chơi, bản build, ảnh chụp cá nhân, log hoặc thông tin đăng nhập.
- Không đổi định dạng save nếu chưa có cách đọc bản cũ và kiểm thử phục hồi.

## Bằng chứng trước khi phát hành

Ghi commit đã kiểm tra, phiên bản Windows/JDK, kết quả `summary.json`, các bài thất bại và giới hạn chưa xác nhận. Kiểm tra trực tiếp gói ZIP trên một thư mục mới trước khi đưa vào Releases. Không coi build thành công hoặc PASS dữ liệu là bằng chứng đồ họa đã đẹp và đúng.
