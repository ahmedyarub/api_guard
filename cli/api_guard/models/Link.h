#ifndef LINK_H
#define LINK_H

#include "Consumer.h"
#include "Producer.h"

class Link
{
public:
    Consumer consumer;
    Producer producer;
};

#endif //LINK_H
