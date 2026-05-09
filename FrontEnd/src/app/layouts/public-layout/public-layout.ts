import { CommonModule } from '@angular/common';
import { AfterViewInit, Component, OnDestroy } from '@angular/core';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet
} from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { TemplateAssetsService } from '../../core/services/template-assets.service';

declare const AOS: any;
declare const PureCounter: any;

const PUBLIC_STYLES: string[] = [
  'assets/frontoffice/vendor/bootstrap/css/bootstrap.min.css',
  'assets/frontoffice/vendor/bootstrap-icons/bootstrap-icons.css',
  'assets/frontoffice/vendor/aos/aos.css',
  'assets/frontoffice/vendor/fontawesome-free/css/all.min.css',
  'assets/frontoffice/vendor/swiper/swiper-bundle.min.css',
  'assets/frontoffice/vendor/glightbox/css/glightbox.min.css',
  'assets/frontoffice/css/main.css'
];

const PUBLIC_SCRIPTS: string[] = [
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
  selector: 'app-public-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss'
})
export class PublicLayoutComponent implements AfterViewInit, OnDestroy {
  private routerSub?: Subscription;
  private initialized = false;

  constructor(
    private router: Router,
    private templateAssetsService: TemplateAssetsService
  ) {}

  async ngAfterViewInit(): Promise<void> {
    if (this.initialized) return;
    this.initialized = true;

    this.templateAssetsService.clearAll();

    document.body.classList.remove('backoffice-body', 'frontoffice-body');
    document.body.classList.add('public-body');

    await this.templateAssetsService.loadGroup(
      'public',
      PUBLIC_STYLES,
      PUBLIC_SCRIPTS
    );

    this.bindUiEvents();
    this.refreshTemplateUi();

    this.routerSub = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        setTimeout(() => {
          this.closeMobileNav();
          this.refreshTemplateUi();
          window.scrollTo({ top: 0, behavior: 'auto' });
        }, 120);
      });
  }

  private bindUiEvents(): void {
    window.addEventListener('scroll', this.onWindowScroll);
    document.addEventListener('click', this.onDocumentClick);
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
          duration: 600,
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
    const body = document.body;
    const header = document.querySelector('#header');

    if (!header) return;

    if (window.scrollY > 100) {
      body.classList.add('scrolled');
    } else {
      body.classList.remove('scrolled');
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

  private closeMobileNav(): void {
    document.body.classList.remove('mobile-nav-active');

    const toggleIcon = document.querySelector('.mobile-nav-toggle');
    if (toggleIcon) {
      toggleIcon.classList.remove('bi-x');
      toggleIcon.classList.add('bi-list');
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

  private onWindowScroll = (): void => {
    this.updateHeaderScrolledState();
    this.updateScrollTopState();
  };

  private onDocumentClick = (event: Event): void => {
    const target = event.target as HTMLElement | null;
    if (!target) return;

    const mobileToggle = target.closest('.mobile-nav-toggle');
    if (mobileToggle) {
      event.preventDefault();
      this.toggleMobileNav();
      return;
    }

    const dropdownToggle = target.closest('.toggle-dropdown');
    if (dropdownToggle) {
      event.preventDefault();

      const parentLi = dropdownToggle.closest('.dropdown');
      const submenu = parentLi?.querySelector('ul');

      parentLi?.classList.toggle('active');
      submenu?.classList.toggle('dropdown-active');

      dropdownToggle.classList.toggle('bi-chevron-down');
      dropdownToggle.classList.toggle('bi-chevron-up');
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
    }
  };

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
    window.removeEventListener('scroll', this.onWindowScroll);
    document.removeEventListener('click', this.onDocumentClick);
    this.templateAssetsService.unloadGroup('public');
    document.body.classList.remove('public-body');
  }
}