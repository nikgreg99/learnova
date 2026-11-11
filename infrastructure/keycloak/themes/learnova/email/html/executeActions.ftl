
<#import "template.ftl" as layout>

<@layout.emailLayout>
    <div style="font-family: Arial, Helvetica, sans-serif;
                color: #202938;
                max-width: 560px;
                margin: 0 auto;
                padding: 32px 24px;">

        <div style="text-align: center; margin-bottom: 36px;">
            <h1 style="font-size: 30px; font-weight: 700;
                       color: #3157D5; margin: 0;">
                Learnova
            </h1>
            <p style="font-size: 13px; color: #718096;">
                Learn. Grow. Achieve.
            </p>
        </div>

        <h2 style="font-size: 24px; color: #202938;">
            Update your password
        </h2>

        <p style="font-size: 15px; line-height: 1.7;
                  color: #4A5568;">
            Hello ${user.firstName!user.username},
        </p>

        <p style="font-size: 15px; line-height: 1.7;
                  color: #4A5568;">
            A password update has been requested for your Learnova
            account. Click the button below to choose a new password.
        </p>

        <div style="text-align: center; margin: 32px 0;">
            <a href="${link}"
               style="display: inline-block;
                      background-color: #3157D5;
                      color: #ffffff;
                      text-decoration: none;
                      padding: 15px 32px;
                      border-radius: 8px;
                      font-size: 15px;
                      font-weight: bold;">
                Reset Password
            </a>
        </div>

        <p style="font-size: 13px; line-height: 1.7;
                  color: #718096;">
            This link expires in ${linkExpirationFormatter(linkExpiration)}.
            If you did not request this change, you can safely ignore
            this email.
        </p>

        <hr style="border: 0; border-top: 1px solid #E2E8F0;
                   margin: 32px 0;">

        <p style="font-size: 12px; line-height: 1.6;
                  color: #A0AEC0; text-align: center;">
            This is an automated message from Learnova.
            Please do not reply to this email.
        </p>
    </div>
</@layout.emailLayout>
