<#macro emailLayout>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${msg("mailTitle")}</title>
</head>
<body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,'Helvetica Neue',Helvetica,sans-serif;color:#1f2937;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#f4f6fb;padding:24px 12px;">
    <tr>
        <td align="center">
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border-radius:18px;overflow:hidden;border:1px solid #ebeef5;">
                <tr>
                    <td style="background:linear-gradient(135deg,#e91e63 0%,#7b1fa2 100%);padding:22px 28px;">
                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                            <tr>
                                <td style="vertical-align:middle;">
                                    <div style="display:inline-block;width:44px;height:44px;line-height:44px;text-align:center;background:#ffffff;border-radius:50%;font-weight:700;color:#7b1fa2;font-size:18px;">NP</div>
                                    <span style="display:inline-block;margin-left:12px;color:#ffffff;font-size:24px;font-weight:700;vertical-align:middle;">NephrosPaidi</span>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td style="padding:30px 30px 14px 30px;font-size:15px;line-height:1.65;">
                        <#nested>
                    </td>
                </tr>
                <tr>
                    <td style="padding:0 30px 30px 30px;">
                        <div style="border-top:1px solid #ebeef5;padding-top:16px;color:#6b7280;font-size:12px;line-height:1.6;">
                            <div>${msg("mailFooterLine1")}</div>
                            <div>${msg("mailFooterLine2")}</div>
                        </div>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
</table>
</body>
</html>
</#macro>
