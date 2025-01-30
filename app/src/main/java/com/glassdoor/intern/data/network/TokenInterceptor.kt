/*
 * Copyright (c) 2025, Glassdoor Inc.
 *
 * Licensed under the Glassdoor Inc Hiring Assessment License.
 * You may not use this file except in compliance with the License.
 * You must obtain explicit permission from Glassdoor Inc before sharing or distributing this file.
 * Mention Glassdoor Inc as the source if you use this code in any way.
 */

package com.glassdoor.intern.data.network

import com.glassdoor.intern.BuildConfig
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import javax.inject.Inject

private const val TOKEN_KEY: String = "token"

/**
 * Done: Declare the email address from your resume as a token
 */
private const val TOKEN_VALUE: String = "pkapadn@clemson.edu"

internal class TokenInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val originalUrl = request.url

        if (originalUrl.toString().endsWith(BuildConfig.ENDPOINT_GET_INFO)) {
            val urlWithToken = originalUrl.newBuilder()
                .addQueryParameter(TOKEN_KEY, TOKEN_VALUE)
                .build()

            request = request.newBuilder().url(urlWithToken).build()
        }

        var response = chain.proceed(request)

        val redirectUrl = response.header("Location")
        if (response.code == 302 && !redirectUrl.isNullOrEmpty()) {
            Timber.w("TokenInterceptor: Redirecting to -> $redirectUrl")

            response.close()

            val redirectedRequest = request.newBuilder().url(redirectUrl).build()
            response = chain.proceed(redirectedRequest)

            if (response.isSuccessful) {
                val responseBodyString = response.body?.string() ?: ""

                Timber.d("TokenInterceptor: Fetched Data -> $responseBodyString")

                return response.newBuilder()
                    .body(responseBodyString.toResponseBody(response.body?.contentType()))
                    .build()
            }
        }

        return response.newBuilder()
            .body(response.body?.string()?.toResponseBody(response.body?.contentType()))
            .build()
    }
}
