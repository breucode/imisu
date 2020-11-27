FROM gcr.io/distroless/base
COPY build/native-image/imisu /imisu
ENTRYPOINT [ "/imisu" ]
