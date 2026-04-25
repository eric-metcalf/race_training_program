// Minimal type shim for @garmin/fitsdk — Garmin's official JS SDK ships JS only.
// We type just the surface we use in `lib/fitParse.ts`.

declare module "@garmin/fitsdk" {
  export class Stream {
    static fromArrayBuffer(buf: ArrayBuffer): Stream;
    static fromByteArray(arr: Uint8Array): Stream;
  }

  export interface DecoderReadOptions {
    applyScaleAndOffset?: boolean;
    expandSubFields?: boolean;
    expandComponents?: boolean;
    convertTypesToStrings?: boolean;
    convertDateTimesToDates?: boolean;
    includeUnknownFields?: boolean;
    mergeHeartRates?: boolean;
  }

  export interface DecoderReadResult {
    messages: Record<string, unknown[]>;
    errors: unknown[];
  }

  export class Decoder {
    constructor(stream: Stream);
    static isFIT(stream: Stream): boolean;
    checkIntegrity(): boolean;
    read(opts?: DecoderReadOptions): DecoderReadResult;
  }
}
