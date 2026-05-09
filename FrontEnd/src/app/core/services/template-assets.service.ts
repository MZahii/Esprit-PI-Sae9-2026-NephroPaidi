import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TemplateAssetsService {
  private styleMap = new Map<string, HTMLLinkElement>();
  private scriptMap = new Map<string, HTMLScriptElement>();
  private readonly assetMarker = 'data-template-asset';

  private findExistingStyle(id: string): HTMLLinkElement | null {
    const mapped = this.styleMap.get(id);
    if (mapped) {
      return mapped;
    }

    const element = document.getElementById(id);
    return element instanceof HTMLLinkElement ? element : null;
  }

  private findExistingScript(id: string): HTMLScriptElement | null {
    const mapped = this.scriptMap.get(id);
    if (mapped) {
      return mapped;
    }

    const element = document.getElementById(id);
    return element instanceof HTMLScriptElement ? element : null;
  }

  loadStyle(id: string, href: string): Promise<void> {
    const existing = this.findExistingStyle(id);

    if (existing) {
      this.styleMap.set(id, existing);
      const currentHref = existing.getAttribute('href');
      if (currentHref === href) {
        return Promise.resolve();
      }

      existing.remove();
      this.styleMap.delete(id);
    }

    return new Promise((resolve, reject) => {
      const link = document.createElement('link');
      link.id = id;
      link.rel = 'stylesheet';
      link.href = href;
      link.setAttribute(this.assetMarker, 'true');

      link.onload = () => {
        this.styleMap.set(id, link);
        resolve();
      };

      link.onerror = () => {
        reject(new Error(`Failed to load style: ${href}`));
      };

      document.head.appendChild(link);
    });
  }

  unloadStyle(id: string): void {
    const link = this.findExistingStyle(id);
    if (link) {
      link.remove();
      this.styleMap.delete(id);
    }
  }

  loadScript(id: string, src: string): Promise<void> {
    const existing = this.findExistingScript(id);

    if (existing) {
      this.scriptMap.set(id, existing);
      const currentSrc = existing.getAttribute('src');
      if (currentSrc === src) {
        return Promise.resolve();
      }

      existing.remove();
      this.scriptMap.delete(id);
    }

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.id = id;
      script.src = src;
      script.async = false;
      script.setAttribute(this.assetMarker, 'true');

      script.onload = () => {
        this.scriptMap.set(id, script);
        resolve();
      };

      script.onerror = () => {
        reject(new Error(`Failed to load script: ${src}`));
      };

      document.body.appendChild(script);
    });
  }

  unloadScript(id: string): void {
    const script = this.findExistingScript(id);
    if (script) {
      script.remove();
      this.scriptMap.delete(id);
    }
  }

  async loadScriptsSequentially(prefix: string, scripts: string[]): Promise<void> {
    for (let i = 0; i < scripts.length; i++) {
      await this.loadScript(`${prefix}-script-${i}`, scripts[i]);
    }
  }

  unloadStylesByPrefix(prefix: string): void {
    [...this.styleMap.keys()]
      .filter(id => id.startsWith(prefix))
      .forEach(id => this.unloadStyle(id));
  }

  unloadScriptsByPrefix(prefix: string): void {
    [...this.scriptMap.keys()]
      .filter(id => id.startsWith(prefix))
      .forEach(id => this.unloadScript(id));
  }

  unloadGroup(prefix: string): void {
    this.unloadStylesByPrefix(prefix);
    this.unloadScriptsByPrefix(prefix);
  }

  async loadGroup(prefix: string, styles: string[], scripts: string[]): Promise<void> {
    for (let i = 0; i < styles.length; i++) {
      await this.loadStyle(`${prefix}-style-${i}`, styles[i]);
    }

    await this.loadScriptsSequentially(prefix, scripts);
  }

  clearAll(): void {
    [...this.styleMap.keys()].forEach(id => this.unloadStyle(id));
    [...this.scriptMap.keys()].forEach(id => this.unloadScript(id));

    document
      .querySelectorAll(`link[${this.assetMarker}="true"], script[${this.assetMarker}="true"]`)
      .forEach(element => element.remove());

    this.styleMap.clear();
    this.scriptMap.clear();
  }

  loadCssFiles(files: string[], prefix = 'css'): Promise<void[]> {
    return Promise.all(
      files.map((file, index) => this.loadStyle(`${prefix}-style-${index}`, file))
    );
  }

  loadJsFiles(files: string[], prefix = 'js'): Promise<void[]> {
    return Promise.all(
      files.map((file, index) => this.loadScript(`${prefix}-script-${index}`, file))
    );
  }
}