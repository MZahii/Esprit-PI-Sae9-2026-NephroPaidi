import { Injectable, OnDestroy } from '@angular/core';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { Observable, Subject, timer } from 'rxjs';
import { filter, share, switchMap, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface PharmacyEvent {
  type: string;
  id?: number;
}

@Injectable({ providedIn: 'root' })
export class PharmacyRealtimeService implements OnDestroy {

  private socket$: WebSocketSubject<PharmacyEvent> | null = null;
  private events$: Observable<PharmacyEvent> | null = null;
  private readonly WS_URL = environment.pharmacyWsUrl;

  private connect(): Observable<PharmacyEvent> {
    if (!this.events$) {
      this.socket$ = webSocket<PharmacyEvent>({
        url: this.WS_URL,
        openObserver: { next: () => console.log('[Pharmacy WS] Connected') },
        closeObserver: { next: () => { console.log('[Pharmacy WS] Disconnected'); this.events$ = null; } }
      });

      this.events$ = this.socket$.pipe(
        tap({ error: () => { this.events$ = null; } }),
        share()
      );
    }
    return this.events$;
  }

  prescriptions$(): Observable<PharmacyEvent> {
    return this.connect().pipe(
      filter(e => e.type === 'PRESCRIPTION_CREATED' || e.type === 'PRESCRIPTION_UPDATED')
    );
  }

  movements$(): Observable<PharmacyEvent> {
    return this.connect().pipe(
      filter(e => e.type === 'MOVEMENT_RECORDED')
    );
  }

  alerts$(): Observable<PharmacyEvent> {
    return this.connect().pipe(
      filter(e => e.type === 'ALERT_UPDATE')
    );
  }

  all$(): Observable<PharmacyEvent> {
    return this.connect();
  }

  ngOnDestroy() {
    this.socket$?.complete();
  }
}
