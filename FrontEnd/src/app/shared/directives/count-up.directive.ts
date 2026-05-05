import { AfterViewInit, Directive, ElementRef, NgZone, OnDestroy } from '@angular/core';

@Directive({
  selector: '[appCountUp]',
  standalone: true
})
export class CountUpDirective implements AfterViewInit, OnDestroy {
  private observer?: MutationObserver;
  private frameId?: number;
  private animating = false;
  private lastTarget = '';
  private pendingTarget = false;

  constructor(
    private readonly elementRef: ElementRef<HTMLElement>,
    private readonly zone: NgZone
  ) {}

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => {
      this.animateFromCurrentText();
      this.observer = new MutationObserver(() => {
        const raw = this.elementRef.nativeElement.textContent?.trim() ?? '';
        if (this.animating) {
          if (raw !== this.lastTarget) {
            this.pendingTarget = true;
          }
          return;
        }
        this.animateFromCurrentText();
      });
      this.observer.observe(this.elementRef.nativeElement, { childList: true, characterData: true, subtree: true });
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    if (this.frameId) {
      cancelAnimationFrame(this.frameId);
    }
  }

  private animateFromCurrentText(): void {
    const element = this.elementRef.nativeElement;
    const raw = element.textContent?.trim() ?? '';
    const parsed = this.parseNumber(raw);

    if (!parsed || raw === this.lastTarget) {
      return;
    }

    this.lastTarget = raw;
    const { value, suffix } = parsed;

    if (value === 0) {
      element.textContent = `0${suffix}`;
      return;
    }

    const start = value > 0 ? 1 : 0;
    const duration = Math.min(1300, Math.max(650, value * 18));
    const startedAt = performance.now();
    this.animating = true;

    const tick = (now: number) => {
      const progress = Math.min(1, (now - startedAt) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      const current = Math.round(start + (value - start) * eased);
      element.textContent = `${current}${suffix}`;

      if (progress < 1) {
        this.frameId = requestAnimationFrame(tick);
        return;
      }

      this.animating = false;
      element.textContent = `${value}${suffix}`;

      if (this.pendingTarget) {
        this.pendingTarget = false;
        requestAnimationFrame(() => this.animateFromCurrentText());
      }
    };

    this.frameId = requestAnimationFrame(tick);
  }

  private parseNumber(raw: string): { value: number; suffix: string } | null {
    const normalized = raw.replace(/\s+/g, '');
    const match = normalized.match(/^(\d+)(%)?$/);
    if (!match) {
      return null;
    }

    return {
      value: Number(match[1]),
      suffix: match[2] ?? ''
    };
  }
}
