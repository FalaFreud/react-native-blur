import React, {Component} from "react";
import {View, NativeModules, Platform, findNodeHandle} from "react-native";

const {BlurManagerModule} = NativeModules;

type Options = {
    width?: number,
    height?: number,
    format: "png" | "jpg" | "webm",
    quality: number,
    result: "tmpfile" | "base64" | "data-uri",
    snapshotContentContainer: boolean
};

if (!BlurManagerModule) {
    console.warn(
        "NativeModules.BlurManagerModule is undefined. Make sure the library is linked on the native side."
    );
}

const acceptedFormats = ["png", "jpg"].concat(
    Platform.OS === "android" ? ["webm"] : []
);

const acceptedResults = ["tmpfile", "base64", "data-uri"];

const defaultOptions = {
    format: "png",
    quality: 1,
    result: "tmpfile",
    snapshotContentContainer: false
};

// validate and coerce options
function validateOptions(
    options: ?Object
): { options: Options, errors: Array<string> } {
    options = {
        ...defaultOptions,
        ...options
    };
    const errors = [];
    if (
        "width" in options &&
        (typeof options.width !== "number" || options.width <= 0)
    ) {
        errors.push("option width should be a positive number");
        delete options.width;
    }
    if (
        "height" in options &&
        (typeof options.height !== "number" || options.height <= 0)
    ) {
        errors.push("option height should be a positive number");
        delete options.height;
    }
    if (
        typeof options.quality !== "number" ||
        options.quality < 0 ||
        options.quality > 1
    ) {
        errors.push("option quality should be a number between 0.0 and 1.0");
        options.quality = defaultOptions.quality;
    }
    if (typeof options.snapshotContentContainer !== "boolean") {
        errors.push("option snapshotContentContainer should be a boolean");
    }
    if (acceptedFormats.indexOf(options.format) === -1) {
        options.format = defaultOptions.format;
        errors.push(
            "option format is not in valid formats: " + acceptedFormats.join(" | ")
        );
    }
    if (acceptedResults.indexOf(options.result) === -1) {
        options.result = defaultOptions.result;
        errors.push(
            "option result is not in valid formats: " + acceptedResults.join(" | ")
        );
    }
    return {options, errors};
}

export function captureRef(
    view: number | ReactElement<*>,
    optionsObject?: Object
): Promise<string> {

    if (typeof view !== "number") {
        const node = findNodeHandle(view);
        if (!node) {
            return Promise.reject(new Error("findNodeHandle failed to resolve view=" + String(view)));
        }
        view = node;
    }
    const {options, errors} = validateOptions(optionsObject);
    if (__DEV__ && errors.length > 0) {
        console.warn("react-native-blur: bad options:\n" + errors.map(e => `- ${e}`).join("\n"));
    }
    return BlurManagerModule.captureRef(view, options);
}

export function releaseCapture(uri: string): void {
    if (typeof uri !== "string") {
        if (__DEV__) {
            console.warn("Invalid argument to releaseCapture. Got: " + uri);
        }
    } else {
        BlurManagerModule.releaseCapture(uri);
    }
}

export function captureScreen(
    optionsObject = {
        format: "png",
        quality: 0.8,
        result: "tmpfile",
        snapshotContentContainer: false
    }): Promise<string> {

    const {options, errors} = validateOptions(optionsObject);
    if (__DEV__ && errors.length > 0) {
        console.warn("react-native-blur: bad options:\n" + errors.map(e => `- ${e}`).join("\n"));
    }
    return BlurManagerModule.captureScreen(options);
}

export function removeBlurView(): Promise<string> {
    if (Platform.OS === "ios") {
        return BlurManagerModule.removeBlurView();
    }
}

export function showBlurView(): Promise<string> {
    if (Platform.OS === "ios") {
        return BlurManagerModule.showBlurView();
    }
}

export function hideContentWhenApplicationInactive(enable : boolean) {
    BlurManagerModule.hideContentWhenApplicationInactive(enable);
}

export function getBlurredImage(): Promise<string> {

    if (Platform.OS === "android") {
        let blurredImage = BlurManagerModule.getBlurredImage();
        if (blurredImage !== null || blurredImage !== "") {
            return blurredImage;
        } else {
            return "";
        }
    }
}
