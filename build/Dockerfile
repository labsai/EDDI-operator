FROM quay.io/operator-framework/ansible-operator:v0.18.1

LABEL name="EDDI Operator" \
      vendor="RHSCL" \
      version="v0.0.1" \
      release="1" \
      summary="This operator is to deploy EDDI Chatbot in your cluster" \
      description="This operator will deploy EDDI Chatbot to the cluster."

COPY licenses /licenses

COPY watches.yaml ${HOME}/watches.yaml

COPY roles/ ${HOME}/roles/

COPY playbook.yaml ${HOME}
