# Kiến trúc và giới hạn hiện tại

## Luồng chính

Đầu vào chuột/phím → `GladeApp` → mô hình `GladeDesign` → dựng cảnh qua `GladeArt` và các bộ dựng kiến trúc → hiển thị.

| Phần | Trách nhiệm |
| --- | --- |
| GladeApp | Điều phối thao tác, lựa chọn, camera và giao diện trong game |
| GladeDesign | Trạng thái cảnh và các quan hệ mount/support |
| AssemblyClipboard | Dán cụm công trình, cấp lại ID và liên kết giữa các phần |
| ArchitectureJoin / StructureShell | Giao cắt và biên hình học công trình |
| WalkController | Di chuyển ở tầm mắt và kiểm tra va chạm |
| WorldFiles | Đọc/ghi và xác minh bản lưu |

`GladeApp` vẫn lớn; lần tách AssemblyClipboard chỉ là bước đầu. Tiếp theo cần tách điều phối công cụ, giao diện HUD và phiên làm việc thành từng thay đổi riêng có kiểm thử hồi quy. Không chuyển toàn bộ lớp sang nhiều file cùng lúc vì dễ làm sai trạng thái chọn/undo/lưu.

## Kiểm thử

Workflow mặc định chạy kiểm thử không cần cửa sổ game. Workflow graphics là lượt chạy thủ công trên máy Windows có desktop và OpenGL, dùng nhãn runner `dreamhouse-graphics`. Phải cấu hình runner đó trước; thêm workflow không có nghĩa đã chạy hoặc đã đạt.

Kết quả từng lần chạy nằm trong `game-code/glade/test-results/<id>/summary.json`. CI chỉ tải summary và log, không tải classes hoặc dữ liệu người chơi.

## Giới hạn công bố

- Windows là nền tảng đã triển khai; chưa tuyên bố hỗ trợ đa nền tảng.
- Kiểm thử tự động không đánh giá đầy đủ thẩm mỹ hoặc cảm giác thao tác.
- Gói Releases và đầu nhánh phát triển có thể khác nhau; xem tag và CHANGELOG trước khi so sánh.
- Chưa công bố giấy phép tái sử dụng toàn bộ dự án. Cần xác minh quyền của hình nền launcher; giữ thông báo giấy phép của thư viện phụ thuộc.
