import { SurvivorConfig, ZoneChapter } from "../model/Types";

/**
 * 客户端配置加载：从 resources/ 下静态 JSON 读取。真实项目可由 Cocos AssetManager 注入。
 */
export class ConfigLoader {

    async loadSurvivors(): Promise<SurvivorConfig[]> {
        return fetch("/assets/resources/config/survivors.json").then((r) => r.json() as Promise<SurvivorConfig[]>);
    }

    async loadZones(): Promise<ZoneChapter[]> {
        return fetch("/assets/resources/config/zones.json").then((r) => r.json() as Promise<ZoneChapter[]>);
    }
}
