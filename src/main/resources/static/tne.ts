class TNE {

    private connected: boolean = false;
    private channels: Set<string>;
    private callback: () => void;
    private eventSource: EventSource;


    private sseUrl:string;
    private notificationUrl:string;

    constructor(apiKey:string, private sseUrl?:string, private notificationUrl?:string) {
        this.channels = new Set<string>();
        if(sseUrl == undefined) {
            this.sseUrl = "/subscribe";
        }
        if (notificationUrl == undefined) {
            this.notificationUrl = "/notifications";
        }

        this.sseUrl += "?ApiKey=" + apiKey;
        this.notificationUrl += "?ApiKey=" + apiKey;
    }

    connect() {
        if(this.connected) {
            this.disconnect();
        }
        let urlWithParameters = this.sseUrl + "?";

        for (let channel of this.channels) {
            urlWithParameters += "&channel" + encodeURIComponent(channel);
        }
        this.eventSource = new EventSource(urlWithParameters);
        this.eventSource.onopen = this.onSseOpen;
        this.eventSource.onmessage = this.onSseMessage;
        this.connected = true;
        return this;
    }


    disconnect(unsubscribeAll?:boolean = false) {
        this.eventSource?.close();
        this.connected = false;
        if(unsubscribeAll) {
            this.channels.clear();
        }
        return this;
    }

    subscribe(...channels: string[]) {
        const state = this.connected;
        if(state) {
            this.disconnect();
        }

        for (let channel of channels) {
            this.channels.add(channel);
        }

        if(state) {
            this.connect();
        }

        return this;
    }


    unsubscribe(...channels: string[]) {
        const state = this.connected;
        if(state) {
            this.disconnect();
        }

        for (let channel of channels) {
            this.channels.delete(channel);
        }

        if(state) {
            this.connect();
        }

        return this;
    }

    onChange(callback: () => void) {
        this.callback = callback;
        return this;
    }


    private onSseMessage(event: MessageEvent) {
        // event.data
        // GET
    }

    private onSseOpen() {
        fetch()
    }

    private fetchNotifications(channels:string[], fresh:boolean) {
        // GET notifications?ApiKey=channel=...recursive=true
        // Make Ajax Call to the Notification Service
        // ... recursive = fresh
        // if fresh is true, it will replace the notifications.
        // else only change the channel request

    }
}