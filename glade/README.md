# DreamHouse · Glade

Game xây dựng bằng Java và jMonkeyEngine. Nhấp đúp **D:\DreamHouse3D\CHOI-GAME.cmd** để mở bản đóng gói. Bản portable có **DreamHouse.exe**, **app** và **runtime**; giữ cả thư mục khi mang sang máy khác. Người chơi không cần cài JDK, Maven hoặc Docker. Chỉ người phát triển cần JDK khi tạo bản mới; launcher chỉ dựng lại nếu mã nguồn thay đổi.

## Dựng và biến đổi kiến trúc

**Xây dựng** có sáu dạng cơ bản: khối vuông, khối trụ, tường, hàng rào, nền/khối craft và bậc thang. Không lặp các mẫu nhà theo vật liệu trong danh sách cấu trúc.

- Trước khi đặt: khung xem trước dạng giàn, có đường viền chân khối.
- Đặt khối nhà: tự có tường vữa, đá viền góc và mái ngói bốn phía. Tháp trụ có mái chóp.
- Kéo tay mái xuống: mái thu dần, chuyển sang sân thượng có tường răng cưa. Kéo lên để khôi phục mái. Thân nhà vẫn chỉnh kích thước riêng.
- Nhà cao tự chia dải tầng; đặt trên sườn đất tạo chân đỡ. Các khối chữ nhật cùng cao độ có thể nối nhà và mái thành chữ L/T.

## Công cụ trực tiếp

- **Con đường** chỉ vẽ đường. Đường qua nước tạo cầu, qua dốc tạo bậc, cắt tường tạo lối thông và cắt hàng rào tạo cổng.
- **Địa hình**: giữ chuột trái để đắp, chuột phải để hạ. Tay kéo mở rộng mép đất luôn hiện trong chế độ này. Đắp lại để lấp chỗ thấp; hướng dẫn nổi hiển thị theo thao tác.
- **Nước**: bấm đặt một nguồn nước. Nước lan qua vùng thấp, bị địa hình cao và nhà chặn; có mặt nước rơi xuống cốt thấp hơn. Không đào hồ tự động, không ép hình ellipse.
- **Cây/Hoa**: bấm đặt một cây hoặc giữ và rê để trồng theo nét. Cả nét là một lần hoàn tác. Tẩy cây để tạo khoảng trống.
- **Bảng màu**: bật chế độ tô rồi bấm vào vật thể để mở vòng chất liệu và màu. Tường và mái chỉnh độc lập; không di chuyển vật thể khi tô.

Nước tính luồng chảy nội bộ trên lưới 0,65 đơn vị, nhưng mặt hiển thị là bề mặt liên tục: bờ bo mềm, sắc nước chuyển nhẹ theo độ sâu, trong mờ ở mép và gợn sóng nhỏ. Không hiển thị ô vuông hoặc bậc nước kiểu Minecraft. Nước lan ngang hữu hạn và tối đa 8192 ô tính toán cho mỗi nguồn.

## Chuột và phím

Bấm khối để chọn; kéo khối đã chọn để dời. Giữ chuột phải và kéo để xoay cảnh; chuột trái chỉ chọn, dời và dựng vật thể, không xoay cảnh. **WASD hoặc phím mũi tên** di chuyển vùng nhìn theo hướng camera, có tăng/giảm tốc nhẹ. **Giữ chuột giữa và kéo** để trượt cảnh. Tâm xoay đi theo vị trí bạn đã di chuyển, không tự đưa về giữa mảnh đất. Riêng trong Địa hình, chuột phải là hạ đất. Cuộn thu phóng chậm, khoảng 3% mỗi nấc chuột tiêu chuẩn.

**Đi bộ** ở góc phải phía dưới chuyển xuống góc nhìn ngang tầm mắt. WASD/mũi tên đi lại; giữ chuột phải để nhìn quanh; Esc hoặc bấm Về xây dựng để trở về. Độ cao mắt bám mặt đất. Đây là chế độ tham quan, chưa mô phỏng va chạm nhân vật với tường nhà. Nút Máy ảnh vẫn chỉ chụp khi người chơi chủ động bấm.

- Các tay kéo trắng chỉnh chiều ngang, sâu, mái và xoay khối theo cung.
- Ba điểm trên thước bên hông đổi cốt trên, cốt dưới và nâng/hạ toàn khối. Thước đi theo chiều cao khối. Khi hai mặt chạm nhau, độ dày chốt ở 0,04 để tránh sai số gây văng game; vẫn nâng lại và hoàn tác được.
- Hai tay mái nằm ở hai bên mái, tách biệt với tay thân nhà.
- **Ctrl+C / Ctrl+X / Ctrl+V** sao chép, cắt, dán cả cụm liên kết.
- **Ctrl+Z / Ctrl+Y** hoàn tác/làm lại; **Esc** trở về chọn/xoay; **[ / ]** chỉnh cỡ bút đất.

Hộp dụng cụ đóng/mở thanh; Hộp bản thiết kế lưu/mở. Menu ba gạch có cảnh mẫu, đất trống, toàn cảnh và hướng dẫn. Thời gian đổi ánh sáng; Máy ảnh chỉ lưu PNG khi người chơi bấm, tại `%LOCALAPPDATA%/DreamHouse/captures`.

## Phản ứng theo kiến trúc

Các quy tắc từ hai file JSON được triển khai thành hình học và phản ứng trong cảnh:

- Cửa sổ thấp đổi thành cửa; cửa sổ đặt trên mái tạo mái phụ; cửa sổ kề nhau tạo ban công chung.
- Cây/hoa cạnh tường tạo dây leo tới mép mái; xóa cây tạo khoảng trống và cập nhật bóng.
- Nước sát nhà tạo chân đá, rêu; vùng nước đủ lớn có lá súng và có thể có vịt.
- Hàng rào khép kín làm gọn cỏ bên trong; đầu hàng rào bắt vào tường gần đó.
- Khối craft có thể chuyển giữa tấm, ghế và sàn nâng theo chiều cao/vật liệu.

Các phản ứng lấy cảm hứng từ game tham khảo; ghép hình học tùy ý cho mọi khối tròn, mái lệch cao độ và mọi đồ trang trí vẫn chưa phải phép hợp hình tổng quát.

## Lưu và kiểm thử

Định dạng **glade-9** lưu vật liệu, màu, biến thể, cấu kiện gắn mái, liên kết tầng, địa hình và nguồn nước; tiếp tục đọc bản 1–8. Không tự đổi diện mạo của bản lưu cũ.

Hai bài kiểm thử hiện hành trong game-code/glade/checks:

- **BuildMorphCheck**: khung xem trước, nhà có mái, hạ/nâng mái đổi hình hai chiều, tháp tròn, lưu/mở, trồng cây/hoa và hoàn tác.
- **ProceduralMechanicsCheck**: cuộn chậm, sáu cấu trúc, đất trái/phải/mép, nước lan và vật cản, cửa/ban công/mái phụ, dây leo, chân đỡ, cầu/bậc/cổng, hàng rào và ghép nhà.

Các bài kiểm thử giao diện cũ mô tả phiên bản trước. Mô hình và họa tiết được dựng bằng mã của dự án, không trích tài nguyên từ game khác.

Kiểm thử không chụp màn hình: **FreeCameraAndFlattenCheck** kiểm tra WASD/mũi tên, chuột giữa/phải, Đi bộ, thoát chế độ, mất tiêu điểm, ép phẳng bằng hai đầu thước và lưu/hoàn tác; **SoftWaterCheck** kiểm tra bờ mềm, sóng, nguồn nước và hoàn tác. Không tự chụp/lưu ảnh sau mỗi lần sửa; chỉ thực hiện khi người dùng yêu cầu.

## Kiến trúc và vật liệu theo file tham khảo mới

Cập nhật từ Kien_truc_va_Vat_lieu_Tiny_Glade.json ngày 06/11/2026:

- Nhà mới mặc định có vữa sáng, khung gỗ lộ ra và chân móng ốp đá; tháp tròn cũng có chi tiết tương ứng theo chu vi. Không đặt thêm móng đất lên tầng chồng.
- Bảng màu đổi cả chi tiết nổi của gạch, đá và các hàng ván gỗ. Đổi lại Vữa để trở về khung gỗ trên tường sáng. Mái ngói/ván lợp vẫn chọn riêng ở mục Mái.
- Chọn Bảng màu rồi bấm đường đi: vật liệu Đá/Gạch chuyển đoạn qua nước thành cầu xây có lan can thấp; Gỗ hoặc Ban đầu dùng cầu gỗ. Đoạn lên dốc đổi bậc gỗ/đá theo vật liệu.
- Chi tiết mặt ngoài được bỏ ở chỗ ghép nhà, cửa và lối xuyên tường. Khối phẳng và đồ craft không bị bao lại thành tường; các chi tiết được gom hình học để giảm số lượt vẽ.
- Lưu, mở, sao chép và hoàn tác giữ lựa chọn vật liệu trong định dạng glade-9 hiện có.

Các phản ứng đường/cửa/dây leo/móng sát nước/hàng rào từ file tiếp tục dùng hệ thống đang có. Nước giữ bờ mềm và cách đổ nguồn theo yêu cầu mới nhất, không tự đào hồ. Ghép nhà vẫn có giới hạn hình học đã nêu, không khẳng định hợp được mọi kiểu khối bất kỳ.

**ArchitectureMaterialCheck** kiểm tra vật liệu nổi, số nhóm hình học, lưu/hoàn tác, khối phẳng/craft/tầng chồng, lối xuyên nhà, mặt trong khối ghép, cầu và bậc theo vật liệu, tháp trụ. Bài kiểm tra không chụp ảnh.

## Hoàn thiện theo báo cáo review

Khi mở game có menu **Tiếp tục/Phục hồi, Thế giới mới, Mở, Lưu, Cài đặt, Hướng dẫn, Thoát**. Nút Menu trong cảnh mở lại màn hình này. Phần cài đặt có chế độ đồ họa nhẹ/đẹp, âm lượng, âm gió, độ nhạy camera và nút đổi cỡ cửa sổ/toàn màn hình. Âm thanh thao tác và gió được tổng hợp bằng mã. Cửa sổ được đặt tối thiểu 960×640; viewport quá nhỏ có thông báo dự phòng thay cho HUD cũ.

Dữ liệu riêng của người chơi nằm trong **%LOCALAPPDATA%/DreamHouse**: `saves`, `recovery`, `captures`, `logs` và `settings.properties`. Các tệp .glade cũ vẫn ở nguyên chỗ cũ; dùng Mở để chọn chúng. Bản đang chơi tự lưu nền sau khoảng 3 giây ngừng chỉnh sửa hoặc định kỳ khoảng 30 giây, luân phiên 3 phiên bản. Nếu bản gần nhất hỏng, phục hồi thử bản trước đó. Lưu thủ công, tạo cảnh khác và thoát có xử lý thay đổi chưa lưu. Các bản phục hồi không thay thế việc chủ động lưu thiết kế quan trọng.

Ngân sách tệp lưu/nhập: 16 MB, 10.000 công trình, tổng 250.000 điểm nét, mỗi nét tối đa 4096 điểm và dữ liệu địa hình có giới hạn. Khi vượt ngân sách ghi, bản lưu tốt trước đó không bị thay thế. Cảnh hiện tại được giữ nếu nhập tệp thất bại. Các giới hạn này là bảo vệ dữ liệu/khả năng nạp, chưa phải cam kết hiệu năng cho cảnh có 10.000 vật thể.

**Kiểm thử:** chạy `KIEM-TRA.cmd` cho dữ liệu và bộ đệm mô hình, hoặc `KIEM-TRA.cmd -IncludeGraphics` để chạy 17 bài hiện hành có phần giao diện/3D. Kết quả nằm trong `glade/test-results/<lượt>/summary.json`. Mặc định không chụp ảnh. Chỉ dùng `-CaptureScreenshots` khi người dùng yêu cầu. Workflow `.github/workflows/dreamhouse-checks.yml` chuẩn bị cho kiểm thử dữ liệu trên Windows CI, chưa được xác nhận chạy từ repository từ xa.

**Đóng gói:** `Build-DreamHouse.ps1` tạo app-image Windows, không dùng installer. `release/current.json` trỏ bản hiện hành. `Start-DreamHouse.ps1` mở bản đóng gói đã kiểm tra; dùng `-Rebuild` khi phát triển để tạo bản mới. Bản exe đã thử với JAVA_HOME không hợp lệ và PATH không có Java trên máy hiện tại; còn cần thử Windows sạch khác.

Bộ đệm SceneRenderer tái sử dụng các mô hình không đổi theo phụ thuộc kiến trúc. Phép đo CPU với 100/500/1000 khối nền đơn giản xác nhận chỉnh 1 khối chỉ dựng lại 1 khối, tái sử dụng phần còn lại. Đây không phải FPS trong cảnh lớn có nhà/cây/nước; đo hiệu năng chơi dài và phần cứng khác vẫn còn cần làm.

Mục tiêu hiện tại là bản game/đồ án hoàn thiện trước **12/11/2026**, chưa ưu tiên Steam. Xem `../KE-HOACH-HOAN-THIEN-DREAMHOUSE.json` cho lịch đề xuất và 15 ý tưởng; xem `../bao-cao-cai-thien-CHOI-GAME.json` cho trạng thái từng mục review và bằng chứng kiểm thử.

## Sửa mái/tường và xác nhận bộ chạy kiểm thử

Mái ghép đã tách mặt ngói và tường đầu hồi. Mặt đầu hồi quay ra ngoài, vật liệu tường không bị đổi thành ngói; các điểm ở mép mái dùng cùng quy tắc cao độ để tránh khe lưới. Cửa mái/ống khói trên mái ghép lấy cao độ từ mái chung. Phạm vi kiểm tra hình học gồm cảnh mẫu và cụm chữ L/T cùng cốt.

Bộ chạy kiểm thử giữ handle tiến trình bằng System.Diagnostics.Process và ghi exitCode trước khi giải phóng. Mỗi lượt tự kiểm chứng exit 0, exit lỗi dù có marker PASS và timeout. Chỉ đạt khi không timeout, mã thoát bằng 0 và có marker hoàn tất. Lượt cũ có exitCode rỗng không được coi là xác nhận hồi quy đạt. Bài RoofEnvelopeCheck kiểm tra hướng mặt, vật liệu, cạnh hở và cao độ mái; không chụp ảnh.

## Đợt thao tác theo kế hoạch 7/11

Khi dựng hoặc dời nhà/nền, các đường trắng báo căn mép hoặc căn tâm với khối gần đó. Giữ **Alt** để đặt tự do, thả Alt để bật lại. Bắt dính theo trục công trình và có khoảng giữ nhẹ ở ngưỡng để giảm rung; tháp tròn không bị ép theo mép chữ nhật. Giới hạn mảnh đất xét cả cụm tầng chồng đang dời.

Điểm kéo giữ cỡ nhìn gần ổn định khi thu/phóng. Khung xem trước chồng tầng bám điểm trỏ trên công trình và dùng đúng khối đỡ khi đặt. Bấm mặt bên để chọn công trình; bấm mặt trên để chồng. Cử chỉ kéo chỉnh không nhận thêm xoay/kéo camera hoặc cuộn zoom; thả thao tác rồi dùng camera như bình thường. Esc hủy, mất tiêu điểm giải phóng các nút tạm. Khối kéo về đúng vị trí cũ không tạo bước hoàn tác thừa.

**BuildInteractionCheck** kiểm tra các hành vi trên, cùng 54 phép kéo trục ở 18 cách đặt camera. Lượt hồi quy cuối có 10 bài đạt. Một lượt trước có ca ép phẳng 4,4 không đạt; ca riêng và lượt đầy đủ sau đạt, nhưng nguyên nhân lượt không ổn định chưa được xác định. Cần tiếp tục theo dõi trong kiểm thử dài/người chơi thật; chưa coi kiểm thử tự động là xác nhận toàn bộ cảm giác thao tác.

## Đợt kiến trúc theo kế hoạch 8/11

Khối vuông và tháp tròn cắt bỏ phần tường bị khối liền kề che; phần tường bên ngoài vẫn giữ lại khi ghép lệch. Ô cửa vòm/tròn và cửa chính có lỗ thật qua bề dày tường. Đường xuyên nhà tạo vòm theo cao độ nền, không tạo cửa lơ lửng dưới nhà nâng. Tầng chồng bỏ mặt sàn kép trong vùng giao và giữ phần sân thượng còn lộ.

Thành sân thượng không chắn ngang mép ghép nội bộ. Cửa bị che kín, cửa mái khi hạ mái và cửa trên thân ép phẳng được tạm ẩn; dữ liệu vẫn lưu và xuất hiện lại khi có mặt đỡ phù hợp. Bảng màu và định dạng glade-9 tiếp tục dùng như trước.

**ArchitectureContinuityCheck** kiểm tra ghép sát/lệch, cửa thật và bề dày, vòm lối đi, nhà nâng, tháp tròn, tầng chồng, viền sân thượng, ẩn/hiện cấu kiện, lưu và cập nhật bộ đệm. Toàn bộ 11 bài hồi quy đạt với mã thoát 0. Không chụp ảnh. Phạm vi ưu tiên là cụm L/T cùng cốt và khối cơ bản; chưa xác nhận mọi kiểu giao mái bất kỳ hoặc nghiệm thu cảm giác chơi trực tiếp.

## Đợt thẩm mỹ theo kế hoạch 9/11

Nút **Thời gian** luân phiên Ban ngày, Nắng chiều và Chạng vạng; ánh sáng, bóng và màu nền chuyển êm trong 1,4 giây. Chọn tiếp khi đang chuyển sẽ nối từ trạng thái hiện tại. Chế độ Đẹp có sương xa nhẹ và bóng tiếp xúc giảm gắt; mức Nhẹ bỏ các hiệu ứng màn hình như trước. Chưa thêm đèn đêm hay chu kỳ thời gian tự động.

Vữa giảm tương phản hạt/vết nứt, vữa/gỗ ít phản sáng gắt. Tán cây có sắc độ theo chiều cao, cây và hoa gom theo vật liệu. Trang trí nền cố định được gom riêng, không gom đất/cỏ/nước cần cập nhật. Tọa độ được kiểm tra để cây không lệch chỗ chọn/xóa sau tối ưu.

**Cảnh mẫu** mới là ngôi nhà vữa-ngói nối tháp và vườn nhỏ, gồm 13 vật thể có thể sửa/lưu như thiết kế người chơi. Bản phục hồi/lưu của người chơi vẫn được nạp theo luồng hiện có. **AtmosphereStyleCheck** kiểm tra chuyển/đảo ánh sáng, ngắt chuyển, giới hạn sương, sắc độ/bộ đệm cây, vị trí cây, lưu cảnh mẫu, dựng đồ họa Đẹp/Nhẹ và ghi thời gian khung hình. Toàn bộ bộ kiểm thử hiện có 12 bài, không chụp ảnh. Số đo cảnh mẫu chỉ đại diện máy/camera/cửa sổ đang thử, không xác nhận hiệu năng cho cảnh lớn hay phê duyệt thẩm mỹ bằng mắt.

## Bản thiết kế và học nhanh — mốc kế hoạch 10/11, triển khai 7/11

**Hộp bản thiết kế** có Lưu/Mở thung lũng và **Lưu cụm chọn**. Mẫu lấy khối gốc, tầng chồng và các cấu kiện gắn vào cụm; lưu vật liệu, mái, góc xoay và liên kết. Chọn cửa sẽ lấy nhà chủ, chọn riêng tầng trên tách khỏi khối đỡ bên ngoài. Mẫu đặt lại bám đất hiện tại; chưa gom nhiều căn nhà độc lập vào một mẫu.

Chọn mẫu → **Đặt mẫu**, rê chuột xem khung rồi bấm trái đặt cả cụm. Esc hủy; chuột phải vẫn xoay camera. Một lần Ctrl+Z hoàn tác cả cụm. Cụm được giới hạn mép đất và dùng mã mới; các bản đặt độc lập với nhau. Có phân trang, đổi tên, xóa qua hai lần bấm xác nhận; xóa trong hộp giữ nguyên các công trình đã đặt.

Mẫu riêng ở `%LOCALAPPDATA%/DreamHouse/blueprints`, mỗi mẫu là tệp .glade có metadata module.version=1, tương thích dữ liệu glade-9. Ghi thay thế nguyên tử; đọc bỏ qua tệp lỗi hoặc quá 2 MB. Giới hạn 256 phần/mẫu, 128 mẫu/hộp và ngân sách điểm/công trình hiện có khi đặt. Thư mục lưu/phục hồi thế giới tiếp tục dùng luồng cũ.

**F1 → Học qua thao tác** bật năm mục tiêu: dựng, biến đổi hình, vật liệu, cất mẫu và lưu thung lũng. Bắt đầu hướng dẫn giữ cảnh hiện tại; có thể tạm ẩn. Thẻ nhường chỗ cho bảng màu; Esc quay lại xem bước tiếp. Tiến độ chỉ theo phiên chơi, có thể khởi động lại qua F1. Xem [hướng dẫn ngắn](../BAT-DAU-CHOI.md).

**BlueprintDataCheck** và **BlueprintFlowCheck** bổ sung kiểm tra dữ liệu/lưu và thao tác thư viện/hướng dẫn. Bộ đầy đủ có 14 bài đạt, không chụp ảnh. Tên mẫu và chọn tệp dùng hộp thoại Swing hiện có; nhánh thành công được kiểm tra qua lưu thật và callback dùng chung, còn hộp thoại/DPI và thử người mới cần xác nhận trực tiếp. Không coi kiểm thử tự động là nghiệm thu trải nghiệm hoặc máy khác.

## M0 · Phần 1 — Launcher Java

`CHOI-GAME.cmd` và `DreamHouse.exe` mở launcher Java trong cùng bản portable. Launcher có hình minh họa và icon vẽ từ bộ biểu tượng của game, nút có phản hồi rê/nhấn/focus, phím Enter, chọn Đẹp/Nhẹ và hướng dẫn nhanh. Bấm **Vào thung lũng** để mở game. Tệp bản lưu và dữ liệu cá nhân tiếp tục ở thư mục riêng.

Trạng thái do game gửi từ khâu chuẩn bị đồ họa, dựng cảnh và khung hình đầu; launcher chỉ bàn giao khi có tín hiệu sẵn sàng. Khóa phiên hệ điều hành tránh mở lặp launcher/game và tự nhả khi tiến trình kết thúc. Lượt mở lỗi trước khi sẵn sàng được dừng để thử lại; lượt tải lâu được chờ tiếp trên cùng tiến trình. Hủy chỉ tác động tới tiến trình do launcher sở hữu trước khi bàn giao. Nhật ký của từng lượt nằm trong `logs` ở dữ liệu người chơi.

Cách khởi chạy hiện dùng bản đóng gói có runtime. Mã nguồn mới hơn không tự khiến máy người chơi phải có JDK. Nhà phát triển dùng `Build-DreamHouse.ps1` hoặc `Start-DreamHouse.ps1 -Rebuild`; dùng `Start-DreamHouse.ps1 -CheckOnly -Console` để kiểm tra tệp phát hành. Trình mở kiểm tra metadata, đường dẫn, jar và runtime trước khi chạy.

**LauncherDataCheck** kiểm tra khóa phiên, hủy sớm, tín hiệu sẵn sàng, lỗi và thử lại, tải lâu, Unicode/khoảng trắng, cài đặt hỏng và icon. **LauncherUiCheck** kiểm tra thành phần Swing ở ba kích thước và trạng thái lỗi, không chụp ảnh. **LauncherFilesCheck** kiểm tra tệp mở game bằng Windows PowerShell. Bộ đầy đủ hiện có 17 bài. Bản portable được thử riêng qua cả launcher và game với PATH không có Java/JAVA_HOME không hợp lệ.

Kiểm tra bố cục bằng thuộc tính không thay cho duyệt thẩm mỹ, thử DPI và hộp thoại bằng chuột thật. Kế hoạch thực hiện từng phần nằm ở `../TIEN-DO-DREAMHOUSE-1-NAM.json`; M0 còn kiểm thử dữ liệu/phục hồi, chơi dài và bản trình bày.
