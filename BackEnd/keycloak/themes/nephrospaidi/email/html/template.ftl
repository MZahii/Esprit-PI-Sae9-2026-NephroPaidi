<#-- Base wrapper for email templates -->
<#macro emailLayout>
<!doctype html>
<html>
<body style="margin:0;padding:0;background:#eef1f7;font-family:Arial,sans-serif;">
  <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#eef1f7;padding:24px;">
    <tr>
      <td align="center">
        <table role="presentation" width="640" cellspacing="0" cellpadding="0" style="background:#ffffff;border-radius:14px;overflow:hidden;">
          <tr>
            <td style="padding:20px 28px;background:linear-gradient(90deg,#e91e63,#7b1fa2);color:#fff;font-size:42px;font-weight:700;">NP NephrosPaidi</td>
          </tr>
          <tr>
            <td style="padding:28px;">
              <#nested>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
</#macro>

