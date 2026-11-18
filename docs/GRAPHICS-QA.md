# Chạy kiểm thử đồ họa

Workflow `DreamHouse graphics` chỉ chạy thủ công trên máy của chủ repo có nhãn `self-hosted`, `Windows`, `X64`, `dreamhouse-graphics`.

1. Trong Settings → Actions → Runners, đăng ký runner Windows theo hướng dẫn GitHub. Không lưu token đăng ký vào repo.
2. Chạy runner trong phiên desktop đã đăng nhập, có driver OpenGL và JDK 21+. Không chạy dưới Windows service không có desktop.
3. Từ Actions → DreamHouse graphics → Run workflow, chọn nhánh đã đọc và tin cậy.
4. Để desktop rảnh trong lượt kiểm thử; không điều khiển chuột/phím làm ảnh hưởng bài kiểm tra.
5. Đọc `summary.json` và log trong artifact `graphics-results`. Không bật chụp ảnh mặc định.

Không chạy mã từ PR bên ngoài trên máy cá nhân. Workflow này không được kích hoạt bằng `pull_request`. Nếu chưa có runner đúng nhãn, lượt chạy sẽ chờ; đây không phải kết quả PASS. Kiểm tra dữ liệu thông thường vẫn chạy độc lập trên máy GitHub.

Có thể kiểm tra tại máy phát triển bằng `KIEM-TRA.cmd -IncludeGraphics`. Cần tiếp tục chơi thử cảnh lớn, ghép nhà và đi bộ; bài tự động không thay thế đánh giá trực quan.
