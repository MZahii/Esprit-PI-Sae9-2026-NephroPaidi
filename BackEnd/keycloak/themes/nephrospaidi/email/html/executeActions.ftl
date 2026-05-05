<#import "template.ftl" as layout>
<@layout.emailLayout>
  <#assign isResetPassword = requiredActions?? && requiredActions?seq_contains("UPDATE_PASSWORD") && !(requiredActions?seq_contains("VERIFY_EMAIL"))>

  <#if isResetPassword>
    <h2 style="margin:0 0 16px 0;color:#1d2433;font-size:42px;font-weight:700;">${msg("passwordResetTitle")}</h2>
    <p style="margin:0 0 6px 0;color:#3a465c;font-size:16px;">${msg("passwordResetIntro")} <strong>${msg("passwordResetAction")}</strong>.</p>
  <#else>
    <h2 style="margin:0 0 16px 0;color:#1d2433;font-size:42px;font-weight:700;">${msg("emailVerificationTitle")}</h2>
    <p style="margin:0 0 6px 0;color:#3a465c;font-size:16px;">${msg("emailVerificationIntro")} <strong>${msg("emailVerificationAction")}</strong>.</p>
  </#if>

  <p style="margin:24px 0;">
    <a href="${link}" style="display:inline-block;background:#e91e63;color:#fff;text-decoration:none;padding:14px 22px;border-radius:12px;font-size:16px;font-weight:700;">
      <#if isResetPassword>
        ${msg("passwordResetButton")}
      <#else>
        ${msg("emailVerificationButton")}
      </#if>
    </a>
  </p>

  <#function readAttr attrName fallbackValue>
    <#assign raw = user.attributes[attrName]!>
    <#if !raw?has_content>
      <#return fallbackValue>
    </#if>
    <#if raw?is_sequence>
      <#assign resolved = raw?first!>
    <#else>
      <#assign resolved = raw>
    </#if>
    <#if !resolved?has_content>
      <#return fallbackValue>
    </#if>
    <#return resolved?string>
  </#function>

  <#assign loginUsername = readAttr("np_login_username", (user.username!"Not provided"))>
  <#assign loginEmail = readAttr("np_login_email", (user.email!"Not provided"))>
  <#assign tempPassword = readAttr("np_temp_password", "Not provided")>

  <#if isResetPassword>
    <div style="border:1px solid #e1e6ef;border-radius:12px;padding:16px;margin:16px 0;">
      <h3 style="margin:0 0 10px 0;color:#1d2433;font-size:18px;">${msg("passwordResetInfoTitle")}</h3>
      <ol style="margin:0 0 0 18px;padding:0;color:#3a465c;line-height:1.8;">
        <li>${msg("passwordResetInfoLine1")}</li>
        <li>${msg("passwordResetInfoLine2")}</li>
        <li>${msg("passwordResetInfoLine3")}</li>
      </ol>
    </div>
  <#else>
    <div style="border:1px solid #e1e6ef;border-radius:12px;padding:16px;margin:16px 0;">
      <h3 style="margin:0 0 10px 0;color:#1d2433;font-size:18px;">${msg("emailVerificationLoginInfoTitle")}</h3>
      <p style="margin:6px 0;color:#3a465c;"><strong>${msg("emailVerificationUsername")}:</strong> ${loginUsername}</p>
      <p style="margin:6px 0;color:#3a465c;"><strong>${msg("emailVerificationEmail")}:</strong> ${loginEmail}</p>
      <p style="margin:6px 0;color:#3a465c;"><strong>${msg("emailVerificationTempPassword")}:</strong> ${tempPassword}</p>
    </div>

    <div style="border:1px solid #e1e6ef;border-radius:12px;padding:16px;margin:16px 0;">
      <h3 style="margin:0 0 10px 0;color:#1d2433;font-size:18px;">${msg("emailVerificationAfterTitle")}</h3>
      <ol style="margin:0 0 0 18px;padding:0;color:#3a465c;line-height:1.8;">
        <li>${msg("emailVerificationAfterStep1")}</li>
        <li>${msg("emailVerificationAfterStep2")}</li>
        <li>${msg("emailVerificationAfterStep3")}</li>
      </ol>
    </div>
  </#if>

  <p style="margin:20px 0 0 0;color:#6d788b;">${msg("emailVerificationExpiry")}</p>
  <p style="margin:8px 0 0 0;color:#6d788b;">${msg("emailVerificationIgnore")}</p>
</@layout.emailLayout>
