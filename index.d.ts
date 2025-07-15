declare const ReactNativeForegroundService: {
  register: ({
    config,
  }: {
    config: {
      alert: boolean;
      onServiceErrorCallBack: () => void;
    };
  }) => void;
  start: ({
    id,
    title,
    message,
    vibration,
    visibility,
    icon,
    largeIcon,
    importance,
    number,
    button,
    buttonText,
    buttonOnPress,
    button2,
    button2Text,
    button2OnPress,
    button3,
    button3Text,
    button3OnPress,
    mainOnPress,
    progress,
    color,
    setOnlyAlertOnce,
    ServiceType,
  }: {
    id: any;
    title?: any;
    message?: string | undefined;
    vibration?: boolean | undefined;
    visibility?: string | undefined;
    icon?: string | undefined;
    largeIcon?: string | undefined;
    importance?: string | undefined;
    number?: string | undefined;
    button?: boolean | undefined;
    buttonText?: string | undefined;
    buttonOnPress?: string | undefined;
    button2?: boolean | undefined;
    button2Text?: string | undefined;
    button2OnPress?: string | undefined;
    button3?: boolean | undefined;
    button3Text?: string | undefined;
    button3OnPress?: string | undefined;
    mainOnPress?: string | undefined;
    progress?: {
      max: number;
      curr: number;
    };
    color?: string;
    setOnlyAlertOnce?: string;
    ServiceType: string;
  }) => Promise<void>;
  update: ({
    id,
    title,
    message,
    vibration,
    visibility,
    largeIcon,
    icon,
    importance,
    number,
    button,
    buttonText,
    buttonOnPress,
    button2,
    button2Text,
    button2OnPress,
    button3,
    button3Text,
    button3OnPress,
    mainOnPress,
    progress,
    color,
    setOnlyAlertOnce,
    ServiceType
  }: {
    id: any;
    title?: any;
    message?: string | undefined;
    vibration?: boolean | undefined;
    visibility?: string | undefined;
    largeIcon?: string | undefined;
    icon?: string | undefined;
    importance?: string | undefined;
    number?: string | undefined;
    button?: boolean | undefined;
    buttonText?: string | undefined;
    buttonOnPress?: string | undefined;
    button2?: boolean | undefined;
    button2Text?: string | undefined;
    button2OnPress?: string | undefined;
    button3?: boolean | undefined;
    button3Text?: string | undefined;
    button3OnPress?: string | undefined;
    mainOnPress?: string | undefined;
    progress?: {
      max: number;
      curr: number;
    };
    color?: string;
    setOnlyAlertOnce?: string;
    ServiceType: string;
  }) => Promise<void>;
  stop: () => Promise<any>;
  stopAll: () => Promise<any>;
  is_running: () => boolean;
  add_task: (
    task: any,
    {
      delay,
      onLoop,
      taskId,
      onSuccess,
      onError,
    }: {
      delay?: number | undefined;
      onLoop?: boolean | undefined;
      taskId?: string | undefined;
      onSuccess?: (() => void) | undefined;
      onError?: ((e) => void) | undefined;
    },
  ) => string;
  update_task: (
    task: any,
    {
      delay,
      onLoop,
      taskId,
      onSuccess,
      onError,
    }: {
      delay?: number | undefined;
      onLoop?: boolean | undefined;
      taskId?: string | undefined;
      onSuccess?: (() => void) | undefined;
      onError?: (() => void) | undefined;
    },
  ) => string;
  remove_task: (taskId: any) => void;
  is_task_running: (taskId: any) => boolean;
  remove_all_tasks: () => {};
  get_task: (taskId: any) => any;
  get_all_tasks: () => {};
  eventListener: (callBack: any) => () => void;
  updateMediaDisplayState: (state: 'playing' | 'paused' | 'stopped') => Promise<string>;
};
export default ReactNativeForegroundService;
