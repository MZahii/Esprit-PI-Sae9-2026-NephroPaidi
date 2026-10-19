import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { BackofficeLayoutComponent } from './backoffice-layout/backoffice-layout';
import { FrontofficeLayoutComponent } from './frontoffice-layout/frontoffice-layout.component';
import { PublicLayoutComponent } from './public-layout/public-layout';
import { clearSession, flushAllWithData, seedSession } from '../testing/test-helpers';

const STAFF_ROLES = ['ADMIN', 'HR', 'DOCTOR', 'NURSE', 'SURGEON', 'PHARMACIST', 'RECEPTIONIST', 'LAB_AGENT'];

const click = () => new MouseEvent('click', { bubbles: true, cancelable: true });

describe('Layouts — navigation par rôle (SCRUM-13)', () => {
  beforeAll(() => {
    (globalThis as any).IntersectionObserver ??= class { observe() {} unobserve() {} disconnect() {} };
  });

  beforeEach(() => vi.useFakeTimers({ shouldAdvanceTime: true }));

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.clearAllTimers();
    vi.useRealTimers();
    clearSession();
  });

  function mount(role: string) {
    seedSession(role);
    TestBed.configureTestingModule({
      imports: [BackofficeLayoutComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(BackofficeLayoutComponent);
    fixture.detectChanges();
    flushAllWithData(http);
    return { fixture, cmp: fixture.componentInstance, http };
  }

  it.each(STAFF_ROLES)('backoffice : le rôle %s obtient son menu et son en-tête', async (role) => {
    const { fixture, cmp, http } = mount(role);
    await vi.advanceTimersByTimeAsync(100);
    flushAllWithData(http);
    fixture.detectChanges();

    expect(cmp.navigationItems.length).toBeGreaterThan(0);
    expect(cmp.navigationItems[0].key).toBe('dashboard');
    expect(cmp.homeRoute).toBe(role === 'PHARMACIST' ? '/backoffice/pharmacy/dashboard' : '/backoffice/dashboard');
    expect(cmp.headerTitle.length).toBeGreaterThan(0);
    expect(cmp.headerSubtitle.length).toBeGreaterThan(0);
    expect(cmp.displayName.length).toBeGreaterThan(0);
    expect(typeof cmp.roleBadgeClass).toBe('string');
    expect(typeof cmp.createAccountToolLabel).toBe('string');
    void cmp.showPageToolsShell;
    void cmp.canViewMyContract;
    void cmp.canUseInternalStaffMessaging;
    fixture.destroy();
  });

  it('backoffice : l’administrateur voit le centre de contrôle', () => {
    const { cmp } = mount('ADMIN');
    expect(cmp.isAdmin).toBe(true);
    expect(cmp.headerTitle).toBe('Admin Control Center');
  });

  it('backoffice : ouverture/fermeture des menus et panneaux', async () => {
    const { fixture, cmp, http } = mount('ADMIN');
    const withChildren = cmp.navigationItems.find((i) => i.children?.length);
    if (withChildren) {
      cmp.toggleMenu(click(), withChildren.key);
      await vi.advanceTimersByTimeAsync(5);
      expect(cmp.isMenuOpen(withChildren.key)).toBe(true);
      cmp.toggleMenu(click(), withChildren.key);
      await vi.advanceTimersByTimeAsync(5);
      expect(cmp.isMenuOpen(withChildren.key)).toBe(false);
      expect(cmp.isItemRouteActive(withChildren)).toBe(false);
    }

    cmp.toggleUserMenu(click());
    expect(cmp.userMenuOpen).toBe(true);
    cmp.toggleMessages(click());
    expect(cmp.messagesOpen).toBe(true);
    expect(cmp.userMenuOpen).toBe(false);

    cmp.openStatsPanel(click());
    expect(cmp.statsPanelOpen).toBe(true);
    cmp.openCreateHrPanel(click());
    expect(cmp.statsPanelOpen).toBe(false);
    expect(cmp.createHrPanelOpen).toBe(true);
    cmp.openCreateContractPanel(new Event('x'));
    expect(cmp.createContractPanelOpen).toBe(true);
    cmp.openStaffDetailsPanel(new CustomEvent('x', { detail: { userId: 0 } }));
    expect(cmp.staffDetailsPanelOpen).toBe(false);
    cmp.openStaffDetailsPanel(new CustomEvent('x', { detail: { userId: 4 } }));
    expect(cmp.staffDetailsPanelOpen).toBe(true);
    expect(cmp.staffDetailsUserId).toBe(4);

    // Un clic ailleurs dans le document referme tout.
    cmp.openStatsPanel(click());
    cmp.onDocumentClick({ target: document.body, preventDefault() {}, stopPropagation() {} } as unknown as MouseEvent);
    expect(cmp.statsPanelOpen).toBe(false);
    expect(cmp.messagesOpen).toBe(false);

    cmp.closeCreateHrPanel();
    cmp.closeCreateContractPanel();
    cmp.closeStaffDetailsPanel();
    expect(cmp.staffDetailsUserId).toBeNull();

    const pending = cmp.toggleNotifications(click());
    for (let i = 0; i < 5; i++) {
      await vi.advanceTimersByTimeAsync(10);
      flushAllWithData(http);
    }
    await pending;
    fixture.destroy();
  });

  it.each(['GUARDIAN', 'DOCTOR'])('frontoffice : rendu pour %s', async (role) => {
    seedSession(role);
    TestBed.configureTestingModule({
      imports: [FrontofficeLayoutComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(FrontofficeLayoutComponent);
    fixture.detectChanges();
    flushAllWithData(http);
    await vi.advanceTimersByTimeAsync(50);
    fixture.detectChanges();
    expect(fixture.nativeElement.innerHTML.length).toBeGreaterThan(0);
  });

  it.each([null, 'GUARDIAN', 'ADMIN'])('public : rendu pour la session %s', async (role) => {
    if (role) seedSession(role);
    TestBed.configureTestingModule({
      imports: [PublicLayoutComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    });
    const fixture = TestBed.createComponent(PublicLayoutComponent);
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(50);
    expect(fixture.nativeElement.innerHTML.length).toBeGreaterThan(0);
  });
});
