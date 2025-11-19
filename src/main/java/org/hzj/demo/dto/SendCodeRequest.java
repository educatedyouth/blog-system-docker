package org.hzj.demo.dto;

// (为了简单，我们暂时不加 @NotEmpty 等校验)
public class SendCodeRequest {
    private String phone;
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
