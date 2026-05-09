package tn.esprit.spring.pharmacyservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.esprit.spring.pharmacyservice.dto.ReorderAlertDTO;
import tn.esprit.spring.pharmacyservice.entity.Supplier;
import tn.esprit.spring.pharmacyservice.entity.SupplyOrder;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    // ─── Order Placed ─────────────────────────────────────────────────────────

    @Async
    public void sendOrderPlacedEmail(Supplier supplier, SupplyOrder order, String medicationName) {
        String to = resolveRecipient(supplier);
        if (to == null) return;

        String subject = "[NephroPaidi Pharmacy] New Supply Order Placed — " + medicationName;
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#f9fafb;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
              <div style="background:linear-gradient(135deg,#2563eb,#1d4ed8);padding:28px 32px;">
                <h2 style="color:#fff;margin:0;font-size:20px;">New Supply Order Placed</h2>
                <p style="color:#bfdbfe;margin:6px 0 0;font-size:13px;">NephroPaidi Pharmacy System</p>
              </div>
              <div style="padding:28px 32px;">
                <p style="color:#374151;margin:0 0 20px;">Hello <strong>%s</strong>,</p>
                <p style="color:#374151;">A new supply order has been placed and is awaiting fulfillment.</p>
                <table style="width:100%%;border-collapse:collapse;margin:20px 0;background:#fff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
                  <tr style="background:#f3f4f6;">
                    <th style="padding:10px 16px;text-align:left;color:#6b7280;font-size:12px;text-transform:uppercase;">Field</th>
                    <th style="padding:10px 16px;text-align:left;color:#6b7280;font-size:12px;text-transform:uppercase;">Value</th>
                  </tr>
                  <tr><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Order ID</td><td style="padding:12px 16px;color:#111827;font-weight:600;border-top:1px solid #e5e7eb;">#%d</td></tr>
                  <tr style="background:#f9fafb;"><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Medication</td><td style="padding:12px 16px;color:#111827;font-weight:600;border-top:1px solid #e5e7eb;">%s</td></tr>
                  <tr><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Quantity Ordered</td><td style="padding:12px 16px;color:#111827;font-weight:600;border-top:1px solid #e5e7eb;">%d units</td></tr>
                  <tr style="background:#f9fafb;"><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Order Date</td><td style="padding:12px 16px;color:#111827;font-weight:600;border-top:1px solid #e5e7eb;">%s</td></tr>
                  <tr><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Status</td><td style="padding:12px 16px;border-top:1px solid #e5e7eb;"><span style="background:#fef3c7;color:#92400e;padding:2px 10px;border-radius:20px;font-size:12px;font-weight:600;">PENDING</span></td></tr>
                </table>
                <p style="color:#6b7280;font-size:13px;">Please process this order at your earliest convenience.</p>
              </div>
              <div style="background:#f3f4f6;padding:16px 32px;text-align:center;border-top:1px solid #e5e7eb;">
                <p style="color:#9ca3af;font-size:12px;margin:0;">NephroPaidi — Pediatric Nephrology Platform</p>
              </div>
            </div>
            """.formatted(
                supplier.getName(),
                order.getOrderId(),
                medicationName,
                order.getOrderedQuantity(),
                order.getOrderDate()
        );

        send(to, subject, html);
    }

    // ─── Order Delivered ──────────────────────────────────────────────────────

    @Async
    public void sendOrderDeliveredEmail(Supplier supplier, SupplyOrder order, String medicationName) {
        String to = resolveRecipient(supplier);
        if (to == null) return;

        String subject = "[NephroPaidi Pharmacy] Order Delivered — " + medicationName;
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;background:#f9fafb;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
              <div style="background:linear-gradient(135deg,#059669,#047857);padding:28px 32px;">
                <h2 style="color:#fff;margin:0;font-size:20px;">Order Delivered Successfully</h2>
                <p style="color:#a7f3d0;margin:6px 0 0;font-size:13px;">NephroPaidi Pharmacy System</p>
              </div>
              <div style="padding:28px 32px;">
                <p style="color:#374151;margin:0 0 20px;">Hello <strong>%s</strong>,</p>
                <p style="color:#374151;">Order <strong>#%d</strong> has been marked as <strong>delivered</strong> and stock has been updated automatically.</p>
                <table style="width:100%%;border-collapse:collapse;margin:20px 0;background:#fff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
                  <tr style="background:#f3f4f6;">
                    <th style="padding:10px 16px;text-align:left;color:#6b7280;font-size:12px;text-transform:uppercase;">Field</th>
                    <th style="padding:10px 16px;text-align:left;color:#6b7280;font-size:12px;text-transform:uppercase;">Value</th>
                  </tr>
                  <tr><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Order ID</td><td style="padding:12px 16px;color:#111827;font-weight:600;border-top:1px solid #e5e7eb;">#%d</td></tr>
                  <tr style="background:#f9fafb;"><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Medication</td><td style="padding:12px 16px;color:#111827;font-weight:600;border-top:1px solid #e5e7eb;">%s</td></tr>
                  <tr><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Quantity Received</td><td style="padding:12px 16px;color:#111827;font-weight:600;border-top:1px solid #e5e7eb;">%d units</td></tr>
                  <tr style="background:#f9fafb;"><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Delivery Date</td><td style="padding:12px 16px;color:#111827;font-weight:600;border-top:1px solid #e5e7eb;">%s</td></tr>
                  <tr><td style="padding:12px 16px;color:#374151;border-top:1px solid #e5e7eb;">Status</td><td style="padding:12px 16px;border-top:1px solid #e5e7eb;"><span style="background:#d1fae5;color:#065f46;padding:2px 10px;border-radius:20px;font-size:12px;font-weight:600;">DELIVERED</span></td></tr>
                </table>
                <p style="color:#6b7280;font-size:13px;">Stock levels have been updated. Thank you for the delivery.</p>
              </div>
              <div style="background:#f3f4f6;padding:16px 32px;text-align:center;border-top:1px solid #e5e7eb;">
                <p style="color:#9ca3af;font-size:12px;margin:0;">NephroPaidi — Pediatric Nephrology Platform</p>
              </div>
            </div>
            """.formatted(
                supplier.getName(),
                order.getOrderId(),
                order.getOrderId(),
                medicationName,
                order.getOrderedQuantity(),
                LocalDate.now()
        );

        send(to, subject, html);
    }

    // ─── Low Stock Alert ──────────────────────────────────────────────────────

    @Async
    public void sendLowStockAlertEmail(List<ReorderAlertDTO> alerts, String recipientEmail) {
        if (alerts.isEmpty()) return;

        String rows = buildAlertRows(alerts);
        String subject = "[NephroPaidi Pharmacy] Low Stock Alert — " + alerts.size() + " medication(s) need reorder";
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:640px;margin:0 auto;background:#f9fafb;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
              <div style="background:linear-gradient(135deg,#dc2626,#b91c1c);padding:28px 32px;">
                <h2 style="color:#fff;margin:0;font-size:20px;">⚠ Low Stock Alert</h2>
                <p style="color:#fecaca;margin:6px 0 0;font-size:13px;">NephroPaidi Pharmacy — Immediate attention required</p>
              </div>
              <div style="padding:28px 32px;">
                <p style="color:#374151;margin:0 0 8px;">The following medications have fallen below their minimum stock threshold and require reordering:</p>
                <table style="width:100%%;border-collapse:collapse;margin:20px 0;background:#fff;border-radius:8px;overflow:hidden;border:1px solid #e5e7eb;">
                  <tr style="background:#f3f4f6;">
                    <th style="padding:10px 14px;text-align:left;color:#6b7280;font-size:11px;text-transform:uppercase;">Medication</th>
                    <th style="padding:10px 14px;text-align:center;color:#6b7280;font-size:11px;text-transform:uppercase;">Current Stock</th>
                    <th style="padding:10px 14px;text-align:center;color:#6b7280;font-size:11px;text-transform:uppercase;">Minimum</th>
                    <th style="padding:10px 14px;text-align:center;color:#6b7280;font-size:11px;text-transform:uppercase;">Deficit</th>
                  </tr>
                  %s
                </table>
                <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:14px 18px;margin-top:4px;">
                  <p style="color:#991b1b;margin:0;font-size:13px;font-weight:600;">Action required: Please place supply orders with the respective suppliers as soon as possible.</p>
                </div>
              </div>
              <div style="background:#f3f4f6;padding:16px 32px;text-align:center;border-top:1px solid #e5e7eb;">
                <p style="color:#9ca3af;font-size:12px;margin:0;">NephroPaidi — Pediatric Nephrology Platform · %s</p>
              </div>
            </div>
            """.formatted(rows, LocalDate.now());

        send(recipientEmail, subject, html);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String buildAlertRows(List<ReorderAlertDTO> alerts) {
        StringBuilder sb = new StringBuilder();
        boolean odd = true;
        for (ReorderAlertDTO a : alerts) {
            String bg = odd ? "" : "background:#f9fafb;";
            String deficitColor = a.getDeficit() > 20 ? "#b91c1c" : "#c2410c";
            sb.append("""
                <tr style="%s">
                  <td style="padding:11px 14px;border-top:1px solid #e5e7eb;">
                    <strong style="color:#111827;">%s</strong>
                    <span style="color:#6b7280;font-size:12px;display:block;">%s</span>
                  </td>
                  <td style="padding:11px 14px;text-align:center;border-top:1px solid #e5e7eb;font-weight:700;color:%s;">%d</td>
                  <td style="padding:11px 14px;text-align:center;border-top:1px solid #e5e7eb;color:#374151;">%d</td>
                  <td style="padding:11px 14px;text-align:center;border-top:1px solid #e5e7eb;">
                    <span style="background:#fee2e2;color:#b91c1c;padding:2px 10px;border-radius:20px;font-size:12px;font-weight:700;">-%d</span>
                  </td>
                </tr>
                """.formatted(
                    bg,
                    a.getName(),
                    a.getForm() != null ? a.getForm() : "",
                    deficitColor,
                    a.getCurrentTotalStock(),
                    a.getMinimumStock(),
                    a.getDeficit()
            ));
            odd = !odd;
        }
        return sb.toString();
    }

    /** Use supplier email if set, otherwise fall back to the sender (admin). */
    private String resolveRecipient(Supplier supplier) {
        if (supplier.getEmail() != null && !supplier.getEmail().isBlank()) {
            return supplier.getEmail();
        }
        return fromAddress;
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent to {} — {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
