# Chạy thử luồng tìm và giữ sân

## Chuẩn bị MySQL

Tạo database `daln`, sau đó chạy theo thứ tự:

1. `database/schema.sql`
2. `database/demo-seed.sql`

Seed tạo cơ sở `DALN Sports Hub`, hai sân cầu lông con và mức giá mẫu 120.000 VND/giờ. Kết nối MySQL dùng cấu hình hiện tại trong `application.properties`; schema được tạo bằng script SQL.

## Khởi động

Windows PowerShell (đảm bảo MySQL đã chạy và database đã được tạo):

```powershell
.\mvnw.cmd spring-boot:run
```

Mở `http://localhost:8080`, chọn **Đăng nhập → Tạo tài khoản**, rồi đăng nhập. Tài khoản khách hàng được lưu trong `users`, mật khẩu lưu dưới dạng BCrypt, và role `CUSTOMER` được nối qua `user_roles`. Sau khi đăng nhập, khách hàng được chuyển tới `/user.html` để sửa hồ sơ và xem lịch sử booking. Thông tin Basic Auth được giữ trong `sessionStorage` của tab để dùng qua trang hồ sơ; đăng xuất hoặc đóng tab sẽ xóa phiên phía giao diện.

Xác thực hiện dùng HTTP Basic và stateless; frontend chỉ giữ header trong bộ nhớ của trang, không ghi mật khẩu vào local/session storage. HTTP Basic chỉ nên dùng qua HTTPS khi triển khai ra môi trường có mạng.

## API được nối với giao diện

- `GET /api/v1/sports`
- `GET /api/v1/venues/search?city=TP.%20Hồ%20Chí%20Minh&sportId=2`
- `GET /api/v1/venues/{venueId}/availability?date=YYYY-MM-DD&sportId=2`
- `POST /api/v1/auth/register` tạo tài khoản khách hàng
- `GET /api/v1/auth/me` kiểm tra thông tin/role bằng HTTP Basic
- `PUT /api/v1/auth/me` cập nhật họ tên và số điện thoại
- `GET /api/v1/bookings/mine` xem booking của chính tài khoản đang đăng nhập
- `POST /api/v1/bookings/hold` yêu cầu role `CUSTOMER` và `Idempotency-Key`
- `GET /api/v1/admin/overview` và `GET /api/v1/admin/venues/pending` yêu cầu `SUPER_ADMIN`
- `PUT /api/v1/admin/venues/{id}/decision` duyệt hoặc từ chối cơ sở, yêu cầu `SUPER_ADMIN`

Đăng ký công khai chỉ tạo role `CUSTOMER`. Các URL `/api/v1/owner/**` và `/api/v1/admin/**` đã được chặn theo role tương ứng, nhưng chức năng quản lý chủ sân/admin chưa được xây dựng. Hold khóa các court liên quan theo cây cha-con, kiểm tra overlap trong transaction và tự hết hạn sau 10 phút. MVP dùng khung một giờ và cọc minh họa 30%; chưa có cổng thanh toán, xác nhận webhook hay payout thật.

Role `VENUE_OWNER` và `SUPER_ADMIN` cần được cấp bởi quản trị viên qua quy trình onboarding; không cho phép client tự chọn role khi đăng ký.

### Tạo tài khoản SUPER_ADMIN đầu tiên

Đặt các biến môi trường trước lần khởi động ứng dụng đầu tiên. Tài khoản mặc định dùng email `admin@daln.local` nếu không đặt email; mật khẩu bắt buộc do bạn tự chọn và phải dài ít nhất 12 ký tự.

```powershell
$env:DALN_ADMIN_EMAIL = "admin@daln.local"
$env:DALN_ADMIN_PASSWORD = "your-own-strong-password"
$env:DALN_ADMIN_NAME = "DALN Administrator"
.\mvnw.cmd spring-boot:run
```

Bootstrap chỉ tạo tài khoản nếu email chưa tồn tại. Nếu email đã thuộc tài khoản thường, ứng dụng sẽ dừng thay vì tự nâng quyền. Admin đăng nhập từ trang chủ sẽ được chuyển tới `/admin.html` để xem số liệu và duyệt venue đang chờ.
