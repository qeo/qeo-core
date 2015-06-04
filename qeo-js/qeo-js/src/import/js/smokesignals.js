/*
 * Copyright (c) 2015 - Qeo LLC
 *
 * The source code form of this Qeo Open Source Project component is subject
 * to the terms of the Clear BSD license.
 *
 * You can redistribute it and/or modify it under the terms of the Clear BSD
 * License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
 * for more details.
 *
 * The Qeo Open Source Project also includes third party Open Source Software.
 * See LICENSE file for more details.
 */

/**
 * @license
 * This code is free.  You can redistribute it and/or modify it in any
 * way that you see fit so long as if you redistribute it with any changes, you
 * don't call it the same thing.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */ 
smokesignals={convert:function(c,e){e={};c.on=function(d,a){(e[d]=e[d]||[]).push(a);return c};c.once=function(d,a){function b(){a.apply(c.off(d,b),arguments)}b.h=a;return c.on(d,b)};c.off=function(d,a){for(var b=e[d],f=0;a&&b&&b[f];f++)b[f]!=a&&b[f].h!=a||b.splice(f--,1);f||delete e[d];return c};c.emit=function(d){for(var a=e[d],b=0;a&&a[b];)a[b++].apply(c,a.slice.call(arguments,1));return c};return c}};
