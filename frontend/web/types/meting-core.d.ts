declare module '@meting/core' {
  export default class Meting {
    constructor(server: 'tencent')
    format(enabled?: boolean): this
    playlist(id: string): Promise<string>
    url(id: string, bitrate?: number): Promise<string>
    pic(id: string, size?: number): Promise<string>
  }
}
