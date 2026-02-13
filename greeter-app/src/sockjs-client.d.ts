declare module 'sockjs-client' {
  interface SockJSOptions {
    server?: string;
    transports?: string | string[];
    sessionId?: number | (() => number);
    timeout?: number;
    devel?: boolean;
    debug?: boolean;
    noCredentials?: boolean;
    cookie?: string;
    headers?: { [key: string]: string };
    prefix?: string;
  }

  class SockJS {
    constructor(url: string, protocols?: string | string[] | null, options?: SockJSOptions);
    protocol: string;
    readyState: number;
    url: string;
    onopen: ((event: any) => void) | null;
    onmessage: ((event: any) => void) | null;
    onclose: ((event: any) => void) | null;
    onerror: ((event: any) => void) | null;
    send(data: string): void;
    close(code?: number, reason?: string): void;
    static readonly CONNECTING: number;
    static readonly OPEN: number;
    static readonly CLOSING: number;
    static readonly CLOSED: number;
  }

  export = SockJS;
}
