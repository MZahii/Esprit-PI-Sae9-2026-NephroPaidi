import { Injectable } from '@angular/core';

type SupportedLanguage = 'en' | 'fr' | 'ar';

@Injectable({ providedIn: 'root' })
export class InterfacePreferencesService {
  private language: SupportedLanguage = 'en';
  private applyFrame?: number;
  private readonly textOriginals = new WeakMap<Text, string>();
  private readonly attrOriginals = new WeakMap<Element, Record<string, string>>();

  private readonly dictionaries: Record<Exclude<SupportedLanguage, 'en'>, Record<string, string>> = {
    fr: {
      'Navigation': 'Navigation',
      'Dashboard': 'Tableau de bord',
      'Audit Logs': 'Journaux d’audit',
      'Clinical Audit Logs': 'Journaux d’audit clinique',
      'Accounts': 'Comptes',
      'Staff': 'Personnel',
      'Clinic Resources': 'Ressources cliniques',
      'Patients': 'Patients',
      'Pharmacy': 'Pharmacie',
      'Admin Control Center': 'Centre de contrôle Admin',
      'Manage HR access and supervise staff, patients, and clinic resources.': 'Gérer les accès RH et superviser le personnel, les patients et les ressources cliniques.',
      'Account Settings': 'Paramètres du compte',
      'Manage personal profile, security controls, and system preferences in one secure area.': 'Gérez votre profil, votre sécurité et vos préférences système dans un espace sécurisé.',
      'My Account': 'Mon compte',
      'Profile': 'Profil',
      'Security': 'Sécurité',
      'Preferences': 'Préférences',
      'Profile Information': 'Informations du profil',
      'First Name': 'Prénom',
      'Last Name': 'Nom',
      'Email': 'E-mail',
      'Phone': 'Téléphone',
      'Read-only Account Data': 'Données du compte en lecture seule',
      'Username:': 'Nom d’utilisateur :',
      'Role:': 'Rôle :',
      'CIN:': 'CIN :',
      'Status:': 'Statut :',
      'Save Profile': 'Enregistrer le profil',
      'Security Settings': 'Paramètres de sécurité',
      'Current Password': 'Mot de passe actuel',
      'New Password': 'Nouveau mot de passe',
      'Confirm New Password': 'Confirmer le nouveau mot de passe',
      'Password must be at least 10 characters and include uppercase, lowercase, number, and special character.': 'Le mot de passe doit contenir au moins 10 caractères avec majuscule, minuscule, chiffre et caractère spécial.',
      'Security State': 'État de sécurité',
      'Password Update Required:': 'Changement de mot de passe requis :',
      'Change Password': 'Changer le mot de passe',
      'Language': 'Langue',
      'Theme': 'Thème',
      'Light': 'Clair',
      'Dark': 'Sombre',
      'System': 'Système',
      'English': 'Anglais',
      'French': 'Français',
      'Arabic': 'Arabe',
      'Enable notifications': 'Activer les notifications',
      'Save Preferences': 'Enregistrer les préférences',
      'Notifications': 'Notifications',
      'unread': 'non lues',
      'Loading...': 'Chargement...',
      'No notifications yet.': 'Aucune notification pour le moment.',
      'My Contract': 'Mon contrat',
      'View your contract details': 'Voir les détails de votre contrat',
      'Manage your profile, security, and preferences': 'Gérer votre profil, sécurité et préférences',
      'Logout': 'Déconnexion',
      'Sign out from backoffice': 'Se déconnecter du backoffice',
      'Help': 'Aide',
      'Terms': 'Conditions',
      'Privacy': 'Confidentialité',
      'First login security policy: you must change your password before continuing.': 'Politique de sécurité de première connexion : vous devez changer votre mot de passe avant de continuer.',
      'Preferences updated successfully.': 'Préférences mises à jour avec succès.',
      'Profile updated successfully.': 'Profil mis à jour avec succès.',
      'Password changed successfully.': 'Mot de passe changé avec succès.'
    },
    ar: {
      'Navigation': 'التنقل',
      'Dashboard': 'لوحة التحكم',
      'Audit Logs': 'سجلات التدقيق',
      'Clinical Audit Logs': 'سجلات التدقيق السريري',
      'Accounts': 'الحسابات',
      'Staff': 'الموظفون',
      'Clinic Resources': 'موارد العيادة',
      'Patients': 'المرضى',
      'Pharmacy': 'الصيدلية',
      'Admin Control Center': 'مركز تحكم المدير',
      'Manage HR access and supervise staff, patients, and clinic resources.': 'إدارة وصول الموارد البشرية والإشراف على الموظفين والمرضى وموارد العيادة.',
      'Account Settings': 'إعدادات الحساب',
      'Manage personal profile, security controls, and system preferences in one secure area.': 'إدارة الملف الشخصي والأمان وتفضيلات النظام في مكان آمن واحد.',
      'My Account': 'حسابي',
      'Profile': 'الملف الشخصي',
      'Security': 'الأمان',
      'Preferences': 'التفضيلات',
      'Profile Information': 'معلومات الملف الشخصي',
      'First Name': 'الاسم',
      'Last Name': 'اللقب',
      'Email': 'البريد الإلكتروني',
      'Phone': 'الهاتف',
      'Read-only Account Data': 'بيانات الحساب للقراءة فقط',
      'Username:': 'اسم المستخدم:',
      'Role:': 'الدور:',
      'CIN:': 'رقم الهوية:',
      'Status:': 'الحالة:',
      'Save Profile': 'حفظ الملف',
      'Security Settings': 'إعدادات الأمان',
      'Current Password': 'كلمة المرور الحالية',
      'New Password': 'كلمة مرور جديدة',
      'Confirm New Password': 'تأكيد كلمة المرور الجديدة',
      'Password must be at least 10 characters and include uppercase, lowercase, number, and special character.': 'يجب أن تتكون كلمة المرور من 10 أحرف على الأقل وتتضمن حرفًا كبيرًا وصغيرًا ورقمًا ورمزًا.',
      'Security State': 'حالة الأمان',
      'Password Update Required:': 'تحديث كلمة المرور مطلوب:',
      'Change Password': 'تغيير كلمة المرور',
      'Language': 'اللغة',
      'Theme': 'المظهر',
      'Light': 'فاتح',
      'Dark': 'داكن',
      'System': 'النظام',
      'English': 'الإنجليزية',
      'French': 'الفرنسية',
      'Arabic': 'العربية',
      'Enable notifications': 'تفعيل الإشعارات',
      'Save Preferences': 'حفظ التفضيلات',
      'Notifications': 'الإشعارات',
      'unread': 'غير مقروءة',
      'Loading...': 'جار التحميل...',
      'No notifications yet.': 'لا توجد إشعارات بعد.',
      'My Contract': 'عقدي',
      'View your contract details': 'عرض تفاصيل العقد',
      'Manage your profile, security, and preferences': 'إدارة الملف والأمان والتفضيلات',
      'Logout': 'تسجيل الخروج',
      'Sign out from backoffice': 'الخروج من لوحة الإدارة',
      'Help': 'مساعدة',
      'Terms': 'الشروط',
      'Privacy': 'الخصوصية',
      'First login security policy: you must change your password before continuing.': 'سياسة أمان أول دخول: يجب تغيير كلمة المرور قبل المتابعة.',
      'Preferences updated successfully.': 'تم تحديث التفضيلات بنجاح.',
      'Profile updated successfully.': 'تم تحديث الملف بنجاح.',
      'Password changed successfully.': 'تم تغيير كلمة المرور بنجاح.'
    }
  };

  start(): void {
    this.scheduleApply();
  }

  stop(): void {
    if (this.applyFrame) {
      cancelAnimationFrame(this.applyFrame);
      this.applyFrame = undefined;
    }
  }

  setLanguage(language: string): void {
    this.language = this.normalizeLanguage(language);
    document.documentElement.setAttribute('lang', this.language);
    document.documentElement.setAttribute('dir', this.language === 'ar' ? 'rtl' : 'ltr');
    this.scheduleApply();
  }

  translate(value: string): string {
    const normalized = this.normalizeText(value);
    if (this.language === 'en') return normalized;
    return this.dictionaries[this.language][normalized] ?? normalized;
  }

  private applyToDocument(): void {
    this.translateElement(document.body);
  }

  private scheduleApply(): void {
    if (this.applyFrame) {
      cancelAnimationFrame(this.applyFrame);
    }

    this.applyFrame = requestAnimationFrame(() => {
      this.applyFrame = undefined;
      this.applyToDocument();
    });
  }

  private translateElement(root: Element): void {
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    const textNodes: Text[] = [];
    let current = walker.nextNode();
    while (current) {
      textNodes.push(current as Text);
      current = walker.nextNode();
    }

    textNodes.forEach((node) => {
      const parent = node.parentElement;
      if (!parent || this.shouldSkip(parent)) return;
      const original = this.textOriginals.get(node) ?? node.textContent ?? '';
      this.textOriginals.set(node, original);
      const translated = this.translatePreservingWhitespace(original);
      if (node.textContent !== translated) {
        node.textContent = translated;
      }
    });

    root.querySelectorAll('[placeholder], [title], option').forEach((element) => {
      if (this.shouldSkip(element)) return;
      this.translateAttributes(element);
    });
  }

  private translateAttributes(element: Element): void {
    const stored = this.attrOriginals.get(element) ?? {};
    ['placeholder', 'title'].forEach((attr) => {
      const current = element.getAttribute(attr);
      if (!current) return;
      stored[attr] = stored[attr] ?? current;
      element.setAttribute(attr, this.translate(stored[attr]));
    });

    if (element.tagName.toLowerCase() === 'option') {
      const original = stored['text'] ?? element.textContent ?? '';
      stored['text'] = original;
      element.textContent = this.translatePreservingWhitespace(original);
    }

    this.attrOriginals.set(element, stored);
  }

  private shouldSkip(element: Element): boolean {
    const tag = element.tagName.toLowerCase();
    return ['script', 'style', 'input', 'textarea', 'code'].includes(tag)
      || element.closest('[data-no-translate]') !== null;
  }

  private translatePreservingWhitespace(value: string): string {
    const prefix = value.match(/^\s*/)?.[0] ?? '';
    const suffix = value.match(/\s*$/)?.[0] ?? '';
    const normalized = this.normalizeText(value);
    return normalized ? `${prefix}${this.translate(normalized)}${suffix}` : value;
  }

  private normalizeText(value: string): string {
    return value.replace(/\s+/g, ' ').trim();
  }

  private normalizeLanguage(language: string): SupportedLanguage {
    if (language === 'fr' || language === 'ar') return language;
    return 'en';
  }
}
