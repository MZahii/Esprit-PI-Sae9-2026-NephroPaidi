import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { AuthStorageService } from '../../core/auth/auth-storage.service';
import { logout } from '../../core/auth/keycloak.service';
import { TemplateAssetsService } from '../../core/services/template-assets.service';

declare const window: any;
declare const AOS: any;
declare const PureCounter: any;

const FRONTOFFICE_STYLES: string[] = [
  'assets/frontoffice/vendor/bootstrap/css/bootstrap.min.css',
  'assets/frontoffice/vendor/bootstrap-icons/bootstrap-icons.css',
  'assets/frontoffice/vendor/aos/aos.css',
  'assets/frontoffice/vendor/fontawesome-free/css/all.min.css',
  'assets/frontoffice/vendor/swiper/swiper-bundle.min.css',
  'assets/frontoffice/vendor/glightbox/css/glightbox.min.css',
  'assets/frontoffice/css/main.css'
];

const FRONTOFFICE_SCRIPTS: string[] = [
  'assets/frontoffice/vendor/bootstrap/js/bootstrap.bundle.min.js',
  'assets/frontoffice/vendor/php-email-form/validate.js',
  'assets/frontoffice/vendor/aos/aos.js',
  'assets/frontoffice/vendor/purecounter/purecounter_vanilla.js',
  'assets/frontoffice/vendor/swiper/swiper-bundle.min.js',
  'assets/frontoffice/vendor/imagesloaded/imagesloaded.pkgd.min.js',
  'assets/frontoffice/vendor/isotope-layout/isotope.pkgd.min.js',
  'assets/frontoffice/vendor/glightbox/js/glightbox.min.js',
  'assets/frontoffice/js/main.js'
];

@Component({
  selector: 'app-frontoffice-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './frontoffice-layout.html',
  styleUrl: './frontoffice-layout.scss'
})
export class FrontofficeLayoutComponent implements OnInit, OnDestroy {
  private navSub?: Subscription;
  private bodyClickHandler = this.onDocumentClick.bind(this);
  private scrollHandler = this.onWindowScroll.bind(this);

  userMenuOpen = false;

  get isServicesActive(): boolean {
    const url = this.router.url;
    return url.includes('/frontoffice/pharmacy') || url === '/services' || url.includes('/doctors');
  }

  constructor(
    private templateAssetsService: TemplateAssetsService,
    private router: Router,
    private authStorage: AuthStorageService
  ) {}

  get user(): any | null {
    return this.authStorage.getUser();
  }

  get displayName(): string {
    if (this.user?.firstName && this.user?.lastName) {
      return `${this.user.firstName} ${this.user.lastName}`;
    }

    return this.user?.username ?? 'Guardian';
  }

  get displayEmail(): string {
    return this.user?.email ?? '';
  }

  get userInitials(): string {
    const name = this.displayName.trim();
    if (!name) return 'G';

    const parts = name.split(' ').filter(Boolean);
    if (parts.length === 1) {
      return parts[0].substring(0, 1).toUpperCase();
    }

    return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
  }

  async ngOnInit(): Promise<void> {
    this.templateAssetsService.clearAll();

    document.body.classList.remove('public-body', 'backoffice-body');
    document.body.classList.add('frontoffice-body');

    try {
      await this.templateAssetsService.loadGroup(
        'frontoffice',
        FRONTOFFICE_STYLES,
        FRONTOFFICE_SCRIPTS
      );
    } catch (error) {
      console.error('Frontoffice assets loading error:', error);
    }

    document.addEventListener('click', this.bodyClickHandler);
    window.addEventListener('scroll', this.scrollHandler);

    setTimeout(() => {
      this.refreshTemplateUi();
    }, 150);

    this.navSub = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => {
        this.userMenuOpen = false;
        this.closeMobileNav();

        setTimeout(() => {
          this.refreshTemplateUi();
          window.scrollTo({ top: 0, behavior: 'auto' });
        }, 120);
      });
  }

  toggleUserMenu(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.userMenuOpen = !this.userMenuOpen;
  }

  async onLogout(): Promise<void> {
    this.userMenuOpen = false;
    await logout();
  }

  private refreshTemplateUi(): void {
    this.updateHeaderScrolledState();
    this.updateScrollTopState();
    this.initAOS();
    this.initPureCounter();
  }

  private initAOS(): void {
    try {
      if (typeof AOS !== 'undefined') {
        AOS.init({
          duration: 700,
          easing: 'ease-in-out',
          once: true,
          mirror: false
        });

        if (typeof AOS.refreshHard === 'function') {
          AOS.refreshHard();
        } else if (typeof AOS.refresh === 'function') {
          AOS.refresh();
        }
      }
    } catch (error) {
      console.log('AOS init skipped:', error);
    }
  }

  private initPureCounter(): void {
    try {
      if (typeof PureCounter !== 'undefined') {
        new PureCounter();
      }
    } catch (error) {
      console.log('PureCounter init skipped:', error);
    }
  }

  private updateHeaderScrolledState(): void {
    const header = document.querySelector('#header');
    if (!header) return;

    if (window.scrollY > 60) {
      document.body.classList.add('scrolled');
    } else {
      document.body.classList.remove('scrolled');
    }
  }

  private updateScrollTopState(): void {
    const scrollTop = document.querySelector('#scroll-top');
    if (!scrollTop) return;

    if (window.scrollY > 100) {
      scrollTop.classList.add('active');
    } else {
      scrollTop.classList.remove('active');
    }
  }

  private toggleMobileNav(): void {
    document.body.classList.toggle('mobile-nav-active');

    const toggleIcon = document.querySelector('.mobile-nav-toggle');
    if (toggleIcon) {
      toggleIcon.classList.toggle('bi-list');
      toggleIcon.classList.toggle('bi-x');
    }
  }

  private closeMobileNav(): void {
    document.body.classList.remove('mobile-nav-active');

    const toggleIcon = document.querySelector('.mobile-nav-toggle');
    if (toggleIcon) {
      toggleIcon.classList.remove('bi-x');
      toggleIcon.classList.add('bi-list');
    }
  }

  private onWindowScroll(): void {
    this.updateHeaderScrolledState();
    this.updateScrollTopState();
  }

  private onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement | null;
    if (!target) return;

    const mobileToggle = target.closest('.mobile-nav-toggle');
    if (mobileToggle) {
      event.preventDefault();
      this.toggleMobileNav();
      return;
    }

    const navLink = target.closest('#navmenu a');
    if (navLink && document.body.classList.contains('mobile-nav-active')) {
      this.closeMobileNav();
    }

    const scrollTop = target.closest('#scroll-top');
    if (scrollTop) {
      event.preventDefault();
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
      return;
    }

    if (!target.closest('.guardian-account-menu')) {
      this.userMenuOpen = false;
    }
  }

  ngOnDestroy(): void {
    this.navSub?.unsubscribe();
    document.removeEventListener('click', this.bodyClickHandler);
    window.removeEventListener('scroll', this.scrollHandler);
    this.templateAssetsService.unloadGroup('frontoffice');
    document.body.classList.remove('frontoffice-body');
  }
}
