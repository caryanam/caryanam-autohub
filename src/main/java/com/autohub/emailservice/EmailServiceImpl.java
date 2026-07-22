package com.autohub.emailservice;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.autohub.entity.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendMail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }


    @Override
    public void sendOtp(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Caryanam - Password Reset Request");

            String userName = email.substring(0, email.indexOf('@'));

            String htmlContent = getPasswordResetTemplate()
                    .replace("{{USER_NAME}}", userName)
                    .replace("{{OTP}}", otp);

            helper.setText(htmlContent, true); // true indicates HTML

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String getPasswordResetTemplate() {
        return """
<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Caryanam OTP</title><style>
body{margin:0;background:#f4f4f4;font-family:Arial,Helvetica,sans-serif;padding:30px}
.container{max-width:650px;margin:auto;background:#111;border-radius:16px;overflow:hidden}
.header{background:#EC003F;padding:38px;text-align:center}
.brand{font-size:42px;font-weight:900;letter-spacing:4px;color:#fff;text-transform:uppercase}
.brand span{color:#FFC107}
.slogan{margin-top:12px;color:#fff;font-size:15px;line-height:24px}
.content{padding:40px;color:#fff}
.content p{color:#d5d5d5;line-height:28px}
.otpbox{background:#fff;border-radius:14px;padding:30px;text-align:center;margin:30px 0}
.otp{font-size:42px;font-weight:bold;color:#EC003F;letter-spacing:10px}
.notice{margin-top:20px;background:#1c1c1c;border-left:4px solid #EC003F;padding:18px;border-radius:8px;color:#ccc}
.footer{background:#0b0b0b;padding:24px;text-align:center;color:#999}
.footer a{color:#EC003F;text-decoration:none}
</style></head><body>
<div class="container">
<div class="header">
<div class="brand">CARY<span>A</span>NAM</div>
<div class="slogan">India's most trusted used-car dealer marketplace.<br>Verified inventory across 150+ cities.</div>
</div>
<div class="content">
<h2>Password Reset Request</h2>
<p>Hello <strong>{{USER_NAME}}</strong>,</p>
<p>We received a request to reset the password for your <strong>Caryanam</strong> account. Please use the verification code below to continue.</p>
<div class="otpbox">
<div style="color:#666">YOUR ONE-TIME PASSWORD</div>
<div class="otp">{{OTP}}</div>
<div style="color:#666;margin-top:10px">Valid for 5 minutes</div>
</div>
<div class="notice"><strong>Security Notice:</strong> If you didn't request this password reset, simply ignore this email. Your password will remain unchanged.</div>
<div class="notice"><strong>System Generated Email:</strong> This is an automated email sent by <strong>Caryanam</strong>. Please <strong>do not reply</strong> to this email because this mailbox is not monitored. For assistance, visit <a href="https://www.caryanam.com">www.caryanam.com</a>.</div>
<p style="margin-top:30px">Regards,<br><strong>Caryanam Team</strong></p>
</div>
<div class="footer">
<p>© 2026 Caryanam. All Rights Reserved.</p>
<p>Secure • Reliable • Trusted</p>
</div>
</div></body></html>
""";
    }

}