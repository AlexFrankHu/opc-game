/**
 * 埋点抽象。客户端 SDK 最终会适配 Firebase Analytics + AppsFlyer（TASK-011）。
 * 此处保持 vendor-neutral，方便换 provider。
 */
export interface AnalyticsProvider {
    track(event: string, props: Record<string, unknown>): void;
    setUserId(id: string): void;
    setUserProperty(key: string, value: string | number | boolean): void;
}

export class NoopAnalytics implements AnalyticsProvider {
    track() {}
    setUserId() {}
    setUserProperty() {}
}

export class ConsoleAnalytics implements AnalyticsProvider {
    private userId = "anon";
    track(event: string, props: Record<string, unknown>) {
        console.log(`[track] uid=${this.userId} ${event}`, props);
    }
    setUserId(id: string) { this.userId = id; }
    setUserProperty(k: string, v: string | number | boolean) {
        console.log(`[user_property] ${k}=${v}`);
    }
}

export class Analytics {
    private static instance: AnalyticsProvider = new ConsoleAnalytics();

    static bind(provider: AnalyticsProvider) {
        Analytics.instance = provider;
    }

    static track(event: string, props: Record<string, unknown> = {}) {
        Analytics.instance.track(event, props);
    }

    static setUserId(id: string) {
        Analytics.instance.setUserId(id);
    }

    static setUserProperty(k: string, v: string | number | boolean) {
        Analytics.instance.setUserProperty(k, v);
    }
}
